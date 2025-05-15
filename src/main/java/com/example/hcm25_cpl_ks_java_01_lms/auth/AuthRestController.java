package com.example.hcm25_cpl_ks_java_01_lms.auth;

import com.example.hcm25_cpl_ks_java_01_lms.auth.dto.AuthResponse;
import com.example.hcm25_cpl_ks_java_01_lms.auth.dto.RegisterDTO;
import com.example.hcm25_cpl_ks_java_01_lms.auth.dto.VerifyOtpDTO;
import com.example.hcm25_cpl_ks_java_01_lms.auth.exception.AuthException;
import com.example.hcm25_cpl_ks_java_01_lms.auth.exception.OtpInvalidException;
import com.example.hcm25_cpl_ks_java_01_lms.auth.exception.UserLockedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.hcm25_cpl_ks_java_01_lms.activity.ActivityService;
import com.example.hcm25_cpl_ks_java_01_lms.activity.ActivityType;
import com.example.hcm25_cpl_ks_java_01_lms.config.security.JwtTokenUtils;
import com.example.hcm25_cpl_ks_java_01_lms.otp.OtpService;
import com.example.hcm25_cpl_ks_java_01_lms.user.User;
import com.example.hcm25_cpl_ks_java_01_lms.user.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {
    private static final Logger log = LogManager.getLogger(AuthRestController.class);

    private final JwtTokenUtils jwtTokenUtils;
    private final UserService userService;
    private final OtpService otpService;
    private final ActivityService activityService;

    public AuthRestController(JwtTokenUtils jwtTokenUtils, UserService userService, OtpService otpService, ActivityService activityService) {
        this.jwtTokenUtils = jwtTokenUtils;
        this.userService = userService;
        this.otpService = otpService;
        this.activityService = activityService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) {
        User authenticatedUser = userService.login(user.getUsername(), user.getPassword());
        if (authenticatedUser == null) {
            throw new AuthException("Invalid username or password");
        }
        if (Boolean.TRUE.equals(authenticatedUser.getIsLocked())) {
            throw new UserLockedException("Your account has been locked. Please contact administrator.");
        }
        String token = jwtTokenUtils.generateToken(authenticatedUser);
        boolean isLocal = request.getServerName().equalsIgnoreCase("localhost");
        Cookie cookie = new Cookie("jwt", token);
        cookie.setPath("/");
        cookie.setSecure(!isLocal);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(86400);
        response.addCookie(cookie);
        // Log activity
        String path = request.getRequestURI();
        String action = "Accessed " + request.getRequestURI();
        String method = request.getMethod();
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        activityService.logActivity(
            authenticatedUser.getId(),
            ActivityType.LOGIN,
            action,
            method,
            path,
            ipAddress,
            userAgent
        );
        return ResponseEntity.ok(new AuthResponse<>(true, "Login successful", authenticatedUser.toDTO()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterDTO register, HttpSession session) {
        User user = User.builder()
                .firstName(register.getFirstName())
                .lastName(register.getLastName())
                .username(register.getUsername())
                .email(register.getEmail())
                .password(register.getPassword())
                .is2faEnabled(false)
                .isLocked(false)
                .build();
        userService.checkRegister(user);
        if (!register.getPassword().equals(register.getConfirmPassword())) {
            throw new AuthException("Password and confirm password do not match");
        }
        session.setAttribute("user", user);
        otpService.sendOtp(user.getEmail());
        return ResponseEntity.ok(new AuthResponse<>(true, "OTP sent successfully", null));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpDTO verifyOtpDTO, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            throw new AuthException("Session expired. Please register again.");
        }
        if ("verify".equals(verifyOtpDTO.getAction())) {
            if (verifyOtpDTO.getOtp() == null || verifyOtpDTO.getOtp().isEmpty()) {
                throw new OtpInvalidException("OTP is required");
            }
            if (!otpService.validateOtp(user.getEmail(), Integer.parseInt(verifyOtpDTO.getOtp()))) {
                throw new OtpInvalidException("Invalid OTP");
            }
            userService.register(user);
            session.removeAttribute("user");
            return ResponseEntity.ok(new AuthResponse<>(true, "Registration successful", null));
        }
        if ("resend".equals(verifyOtpDTO.getAction())) {
            otpService.sendOtp(user.getEmail());
            return ResponseEntity.ok(new AuthResponse<>(true, "OTP resent successfully", null));
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.ok("Logged out successfully");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> processForgotPassword(@RequestParam String email, HttpSession session) {
        User user = userService.findByEmail(email);
        if (user == null) {
            throw new AuthException("Email does not exist in the system");
        }
        session.setAttribute("resetEmail", email);
        otpService.sendOtp(email);
        return ResponseEntity.ok(new AuthResponse<>(true, "OTP has been sent to your email", null));
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam String newPassword,
                                         @RequestParam String confirmPassword,
                                         HttpSession session) {
        String email = (String) session.getAttribute("resetEmail");
        if (email == null) {
            throw new AuthException("Session expired. Please try again.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new AuthException("Password and confirm password do not match");
        }
        userService.resetPassword(email, newPassword);
        session.removeAttribute("resetEmail");
        return ResponseEntity.ok(new AuthResponse<>(true, "Password has been reset successfully", null));
    }

    @PostMapping("/switch-role")
    public ResponseEntity<?> switchRole(@RequestParam String newRole,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new AuthException("No cookies found");
        }
        String jwtToken = Arrays.stream(cookies)
                .filter(cookie -> "jwt".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
        if (jwtToken == null) throw new AuthException("No JWT token found");
        List<String> roles = Arrays.asList(jwtTokenUtils.extractRoles(jwtToken).split(";"));

        String newToken = "";
        if(roles.contains("Admin")){
            newToken = jwtTokenUtils.updateCurrentRole(jwtToken, newRole);
        }

        if(roles.contains("Instructor") && !newRole.equals("Admin")){
            newToken = jwtTokenUtils.updateCurrentRole(jwtToken, newRole);
        }

        Cookie newCookie = new Cookie("jwt", newToken);
        newCookie.setPath("/");
        response.addCookie(newCookie);
        return ResponseEntity.ok(new AuthResponse<>(true, "Role switched successfully", null));
    }
}