package com.example.hcm25_cpl_ks_java_01_lms.attendance;

import com.example.hcm25_cpl_ks_java_01_lms.common.Constants;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/attendance/qr")
@PreAuthorize("@customSecurityService.hasRoleForModule(authentication, 'Attendance')")
public class AttendanceQRController {
    private static final Logger logger = LoggerFactory.getLogger(AttendanceQRController.class);

    private final AttendanceTokenService tokenService;
    private final AttendanceService attendanceService;

    /**
     * Hiển thị trang quản lý QR code cho giáo viên
     */
    @GetMapping("/manage/{classId}")
    @PreAuthorize("hasRole('Admin')")
    public String showQRManagementPage(@PathVariable Long classId, Model model) {
        try {
            // Validate class exists
            attendanceService.validateClassExists(classId);

            model.addAttribute("classId", classId);
            model.addAttribute("today", LocalDate.now());
            model.addAttribute("content", "attendance/qr-manage");
            return Constants.LAYOUT;
        } catch (Exception e) {
            logger.error("Error loading QR management page: {}", e.getMessage(), e);
            model.addAttribute("error", e.getMessage());
            return "redirect:/attendance";
        }
    }

    /**
     * API để giảng viên tạo token cho một lớp học
     */
    @PostMapping("/generate")
    @PreAuthorize("hasRole('Admin')")
    @ResponseBody
    public ResponseEntity<?> generateTokens(
            @RequestParam Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<AttendanceTokenDTO> tokens = tokenService.generateTokensForClass(classId, date);
            return ResponseEntity.ok(tokens);
        } catch (Exception e) {
            logger.error("Error generating QR tokens: {}", e.getMessage(), e);
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Hiển thị trang QR code cho giáo viên
     */
    @GetMapping("/view")
    @PreAuthorize("hasRole('Admin')")
    public String showQRCodePage(
            @RequestParam Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {
        try {
            List<AttendanceTokenDTO> tokens = tokenService.generateTokensForClass(classId, date);
            model.addAttribute("tokens", tokens);
            model.addAttribute("classId", classId);
            model.addAttribute("date", date);
            model.addAttribute("content", "attendance/qr-view");
            return Constants.LAYOUT;
        } catch (Exception e) {
            logger.error("Error showing QR codes: {}", e.getMessage(), e);
            model.addAttribute("error", e.getMessage());
            return "redirect:/attendance/qr/manage/" + classId;
        }
    }

    /**
     * Hiển thị trang quét QR code cho học viên
     */
    @GetMapping("/scanner")
    public String showQRScanner(Model model) {
        model.addAttribute("content", "attendance/qr-scanner");
        return Constants.LAYOUT;
    }

    /**
     * Hiển thị trang submit điểm danh khi học viên quét QR code
     */
    @GetMapping("/submit")
    @PreAuthorize("permitAll()")  // Cho phép tất cả truy cập
    public String showSubmitPage(@RequestParam String token, Model model) {
        logger.info("Hiển thị trang submit điểm danh với token: {}", token);

        try {
            // Xác thực token và lấy thông tin
            AttendanceTokenDTO tokenInfo = tokenService.validateToken(token);

            // Thêm dữ liệu vào model
            model.addAttribute("token", token);
            model.addAttribute("studentName", tokenInfo.getStudentName());
            model.addAttribute("expiresAt", tokenInfo.getExpiresAt());
            model.addAttribute("createdAt", tokenInfo.getCreatedAt());

            // Tính thời gian sớm nhất có thể submit (createdAt + 1 phút)
            LocalDateTime earliestSubmitTime = tokenInfo.getCreatedAt().plusSeconds(60);
            model.addAttribute("earliestSubmitTime", earliestSubmitTime);
            model.addAttribute("currentTime", LocalDateTime.now());

            return "attendance/qr-submit";  // Trả về template mới attendance/qr-submit.html
        } catch (Exception e) {
            logger.error("Lỗi khi hiển thị trang submit: {}", e.getMessage(), e);

            // Nếu có lỗi, chuyển hướng tới trang kết quả với thông báo lỗi
            model.addAttribute("success", false);
            model.addAttribute("message", e.getMessage());

            return "attendance/qr-result";
        }
    }

    /**
     * Xử lý submit điểm danh từ form
     */
    @PostMapping("/process-submit")
    @PreAuthorize("permitAll()")  // Cho phép tất cả truy cập
    public String processSubmit(@RequestParam String token, Model model) {
        logger.info("Xử lý submit điểm danh với token: {}", token);

        try {
            // Gọi service để xử lý điểm danh
            AttendanceResultDTO attendance = tokenService.checkAttendance(token);
            logger.info("Xử lý điểm danh thành công cho: {}", attendance.getStudentName());

            // Thêm dữ liệu vào model
            model.addAttribute("success", true);
            model.addAttribute("attendance", attendance);

            return "attendance/qr-result";  // Trả về template kết quả
        } catch (Exception e) {
            logger.error("Lỗi khi xử lý submit điểm danh: {}", e.getMessage(), e);

            // Thiết lập dữ liệu cho trường hợp lỗi
            model.addAttribute("success", false);
            model.addAttribute("message", e.getMessage());

            return "attendance/qr-result";
        }
    }

    /**
     * Xử lý điểm danh khi học viên quét QR code - giờ chuyển hướng tới trang submit
     */
    @GetMapping("/check")
    public String checkAttendance(@RequestParam String token, Model model) {
        logger.info("Redirect từ /check tới /submit với token: {}", token);
        return "redirect:/attendance/qr/submit?token=" + token;
    }

    /**
     * API xử lý điểm danh qua mobile app
     */
    @GetMapping("/api/check")
    @ResponseBody
    public ResponseEntity<?> checkAttendanceApi(@RequestParam String token) {
        try {
            AttendanceResultDTO attendance = tokenService.checkAttendance(token);

            return ResponseEntity.ok(attendance);
        } catch (Exception e) {
            logger.error("Error checking attendance with token via API: {}", e.getMessage(), e);
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/test-result")
    public String testResultPage(Model model) {
        model.addAttribute("success", true);
        model.addAttribute("attendance", new AttendanceResultDTO("HS123", "Nguyễn Văn A", LocalDateTime.now()));
        return "attendance/qr-result";  // KHÔNG dùng Constants.LAYOUT
    }
}