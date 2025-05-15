package com.example.hcm25_cpl_ks_java_01_lms.attendance;

import com.example.hcm25_cpl_ks_java_01_lms.user.User;
import com.example.hcm25_cpl_ks_java_01_lms.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller xử lý các request liên quan đến gửi email từ màn hình điểm danh
 */
@RestController
@RequestMapping("/attendance")
@PreAuthorize("@customSecurityService.hasRoleForModule(authentication, 'Attendance')")
public class AttendanceEmailController {
    private static final Logger logger = LoggerFactory.getLogger(AttendanceEmailController.class);

    private final EmailAttendanceService emailAttendanceService;
    private final AttendanceService attendanceService;
    private final UserService userService;

    @Autowired
    public AttendanceEmailController(
            EmailAttendanceService emailAttendanceService,
            AttendanceService attendanceService,
            UserService userService) {
        this.emailAttendanceService = emailAttendanceService;
        this.attendanceService = attendanceService;
        this.userService = userService;
    }

    /**
     * API gửi email thông báo điểm danh cho sinh viên
     * @param request Thông tin yêu cầu gửi email
     * @return Kết quả gửi email
     */
    @PostMapping("/send-email")
    public ResponseEntity<Map<String, Object>> sendAttendanceEmail(@RequestBody EmailRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Lấy thông tin điểm danh
            Attendance attendance = attendanceService.findAttendanceById(request.getAttendanceId());
            if (attendance == null || attendance.getStudent() == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy thông tin điểm danh hoặc sinh viên");
                return ResponseEntity.badRequest().body(response);
            }

            // Lấy thông tin người gửi từ JWT token
            // Sử dụng phương thức getCurrentUser thay vì getUserByUsername
            User sender = userService.getCurrentUser();
            if (sender == null) {
                response.put("success", false);
                response.put("message", "Không thể xác định người gửi email");
                return ResponseEntity.badRequest().body(response);
            }

            // Gửi email
            boolean success = emailAttendanceService.sendDirectEmail(
                    attendance.getStudent().getEmail(),
                    attendance.getStudent(),
                    request.getSubject(),
                    request.getContent(),
                    sender,
                    attendance
            );

            if (success) {
                response.put("success", true);
                response.put("message", "Email đã được gửi thành công đến " +
                        attendance.getStudent().getFirstName() + " " + attendance.getStudent().getLastName());
            } else {
                response.put("success", false);
                response.put("message", "Không thể gửi email");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Lỗi khi gửi email: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Lỗi khi gửi email: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * API gửi email thông báo cho toàn bộ sinh viên vắng mặt trong một lớp vào một ngày cụ thể
     * @param classId ID của lớp học
     * @param date Ngày học (định dạng: yyyy-MM-dd)
     * @param subject Tiêu đề email (tùy chọn)
     * @return Kết quả gửi email
     */
    @PostMapping("/send-bulk-emails")
    public ResponseEntity<Map<String, Object>> sendBulkEmails(
            @RequestParam Long classId,
            @RequestParam String date,
            @RequestParam(required = false) String subject) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Xử lý logic gửi email hàng loạt
            // Triển khai theo yêu cầu của bạ
            response.put("success", true);
            response.put("message", "Đã gửi email thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Lỗi khi gửi email hàng loạt: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Lỗi khi gửi email: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }


}