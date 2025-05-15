package com.example.hcm25_cpl_ks_java_01_lms.auth;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.example.hcm25_cpl_ks_java_01_lms.activity.ActivityService;
import com.example.hcm25_cpl_ks_java_01_lms.activity.ActivityType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.hcm25_cpl_ks_java_01_lms.config.security.JwtTokenUtils;
import com.example.hcm25_cpl_ks_java_01_lms.otp.OtpService;
import com.example.hcm25_cpl_ks_java_01_lms.user.User;
import com.example.hcm25_cpl_ks_java_01_lms.user.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/auth")
public class AuthController {
    private static final Logger log = LogManager.getLogger(AuthController.class);

    private final JwtTokenUtils jwtTokenUtils;
    private final UserService userService;
    private final OtpService otpService;
    private final ActivityService activityService;

    public AuthController(JwtTokenUtils jwtTokenUtils, UserService userService, OtpService otpService, ActivityService activityService) {
        this.jwtTokenUtils = jwtTokenUtils;
        this.userService = userService;
        this.otpService = otpService;
        this.activityService = activityService;
    }

    @ModelAttribute
    public void redirectIfValid(@ModelAttribute("valid") boolean valid, HttpServletResponse response, HttpServletRequest request) throws java.io.IOException {
        List<String> uriAccepted = Arrays.asList("/auth/logout", "/auth/switch-role");
        String uri = request.getRequestURI();
        if (valid && !uriAccepted.contains(uri)) {
            response.sendRedirect("/dashboard");
        }
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("user", new User());
        return "auths/login";
    }

    @PostMapping("/login")
    public String login(@ModelAttribute User user,
                        Model model,
                        HttpServletRequest request,
                        HttpServletResponse response) {
        try {
            User authenticatedUser = userService.login(user.getUsername(), user.getPassword());
            if (authenticatedUser != null) {
                log.info("authenticatedUser: {}", authenticatedUser);
                if (Boolean.TRUE.equals(authenticatedUser.getIsLocked())) {
                    model.addAttribute("error", "Your account has been locked. Please contact administrator.");
                    return "auths/login";
                }
                
                String token = jwtTokenUtils.generateToken(authenticatedUser);
                log.info("token: {}", token);

                boolean isLocal = request.getServerName().equalsIgnoreCase("localhost");

//                boolean isLocal = request.getServerName().equalsIgnoreCase("localhost");

                Cookie cookie = new Cookie("jwt", token);
                cookie.setPath("/");
                cookie.setSecure(!isLocal);
                cookie.setHttpOnly(true);
                cookie.setMaxAge(86400); // 1 ngày
                response.addCookie(cookie);

                //ghi log
                if (token != null) {
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
                }

                model.addAttribute("user", authenticatedUser);
                return "redirect:/dashboard";
            } else {
                model.addAttribute("error", "Invalid username or password");
                return "auths/login";
            }
        } catch (Exception e) {
            log.error("Error in AuthController.login: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "auths/login";
        }
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new User());
        return "auths/register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute User user,
                           @RequestParam String confirmPassword,
                           HttpSession session,
                           Model model) {
        try {
            userService.checkRegister(user);
            if (!user.getPassword().equals(confirmPassword)) {
                model.addAttribute("error", "Password and confirm password do not match");
                return "auths/register";
            }
            session.setAttribute("user", user);
            model.addAttribute("email", user.getEmail());
            otpService.sendOtp(user.getEmail());
            model.addAttribute("message", "OTP sent successfully");
            return "auths/verify-otp";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "auths/register";
        }
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam(required = false) String otp,
                            @RequestParam String action,
                            HttpSession session,
                            Model model) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            model.addAttribute("error", "Session expired. Please register again.");
            return "auths/register";
        }

        try {
            if (action.equals("verify")) {
                if (otp == null || otp.isEmpty()) {
                    model.addAttribute("email", user.getEmail());
                    model.addAttribute("error", "OTP is required");
                    return "auths/verify-otp";
                } else if (otpService.validateOtp(user.getEmail(), Integer.parseInt(otp))) {
                    userService.register(user);
                    session.removeAttribute("user");
                    return "redirect:/auth/login";
                } else {
                    model.addAttribute("email", user.getEmail());
                    model.addAttribute("error", "Invalid OTP");
                    return "auths/verify-otp";
                }
            }

            if (action.equals("resend")) {
                otpService.sendOtp(user.getEmail());
                model.addAttribute("email", user.getEmail());
                model.addAttribute("message", "OTP sent successfully");
                return "auths/verify-otp";
            }
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "auths/verify-otp";
        }
        return "redirect:/auth/login";
    }


    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/auth/login";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {
        return "auths/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, 
                                      HttpSession session,
                                      Model model) {
        try {
            User user = userService.findByEmail(email);
            if (user == null) {
                model.addAttribute("error", "Email does not exist in the system");
                return "auths/forgot-password";
            }
            
            session.setAttribute("resetEmail", email);
            otpService.sendOtp(email);
            model.addAttribute("email", email);
            model.addAttribute("message", "OTP has been sent to your email");
            return "auths/reset-password-verify";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "auths/forgot-password";
        }
    }
    
    @PostMapping("/verify-reset-otp")
    public String verifyResetOtp(@RequestParam String otp,
                              @RequestParam(required = false) String action,
                              HttpSession session,
                              Model model) {
        String email = (String) session.getAttribute("resetEmail");
        
        if (email == null) {
            model.addAttribute("error", "Session expired. Please try again.");
            return "auths/forgot-password";
        }
        
        try {
            if (action != null && action.equals("resend")) {
                otpService.sendOtp(email);
                model.addAttribute("email", email);
                model.addAttribute("message", "OTP has been resent successfully");
                return "auths/reset-password-verify";
            }
            
            if (otp == null || otp.isEmpty()) {
                model.addAttribute("email", email);
                model.addAttribute("error", "OTP is required");
                return "auths/reset-password-verify";
            }
            
            if (otpService.validateOtp(email, Integer.parseInt(otp))) {
                model.addAttribute("email", email);
                return "auths/reset-password";
            } else {
                model.addAttribute("email", email);
                model.addAttribute("error", "Invalid OTP");
                return "auths/reset-password-verify";
            }
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "auths/reset-password-verify";
        }
    }
    
    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String newPassword,
                              @RequestParam String confirmPassword,
                              HttpSession session,
                              Model model) {
        String email = (String) session.getAttribute("resetEmail");
        
        if (email == null) {
            model.addAttribute("error", "Session expired. Please try again.");
            return "auths/forgot-password";
        }
        
        try {
            if (!newPassword.equals(confirmPassword)) {
                model.addAttribute("error", "Password and confirm password do not match");
                return "auths/reset-password";
            }
            
            userService.resetPassword(email, newPassword);
            session.removeAttribute("resetEmail");
            model.addAttribute("message", "Password has been reset successfully");
            return "redirect:/auth/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "auths/reset-password";
        }
    }

    @PostMapping("/switch-role")
    public String switchRole(@RequestParam String newRole,
                            HttpServletRequest request,
                            HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return "redirect:/";
        }

        String jwtToken = Arrays.stream(cookies)
                .filter(cookie -> "jwt".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);

        if (jwtToken == null) return "redirect:/";

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

//        // 👉 Lấy userId từ JWT token (tùy vào bạn implement extractUserId như thế nào)
//        String userId = String.valueOf(jwtTokenUtils.extractUserId(newToken));

        // 👉 Điều hướng theo vai trò mới
//        if ("Student".equalsIgnoreCase(newRole)) {
//            return "redirect:/student/courses";
//        }

        return "redirect:/";
    }
}
