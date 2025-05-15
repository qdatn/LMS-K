package com.example.hcm25_cpl_ks_java_01_lms.attendance;

import com.example.hcm25_cpl_ks_java_01_lms.email.EmailService;
import com.example.hcm25_cpl_ks_java_01_lms.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * Dịch vụ xử lý các chức năng gửi email liên quan đến điểm danh
 */
@Service
public class EmailAttendanceService {
    private static final Logger logger = LoggerFactory.getLogger(EmailAttendanceService.class);

    private final EmailService emailService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Autowired
    public EmailAttendanceService(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Gửi email thông báo trạng thái điểm danh cho sinh viên
     * @param attendance Bản ghi điểm danh
     */
    @Async
    public void sendAttendanceStatusNotification(Attendance attendance) {
        if (attendance.getStudent() == null || attendance.getStudent().getEmail() == null) {
            logger.warn("Không thể gửi email - sinh viên hoặc email không tồn tại");
            return;
        }

        String studentName = attendance.getStudent().getFirstName() + " " + attendance.getStudent().getLastName();
        String className = attendance.getClassInfo().getName();
        String classCode = attendance.getClassInfo().getClassCode();
        String date = attendance.getAttendanceDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        boolean isPresent = attendance.getIsPresent() != null && attendance.getIsPresent();
        boolean isExcused = attendance.getIsExcused() != null && attendance.getIsExcused();

        // Tạo tiêu đề email
        String subject = "Thông báo điểm danh lớp " + className;

        // Tạo nội dung email
        StringBuilder messageText = new StringBuilder();
        messageText.append("Xin chào ").append(studentName).append(",\n\n");
        messageText.append("Đây là thông báo về trạng thái điểm danh của bạn trong buổi học ngày ").append(date);
        messageText.append(" tại lớp ").append(className).append(" (Mã lớp: ").append(classCode).append(").\n\n");

        if (isPresent) {
            messageText.append("Trạng thái điểm danh: Có mặt\n");
            if (attendance.getCheckedInTime() != null) {
                messageText.append("Thời gian điểm danh: ")
                        .append(attendance.getCheckedInTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append("\n");
            }
        } else {
            if (isExcused) {
                messageText.append("Trạng thái điểm danh: Vắng có phép\n");
            } else {
                messageText.append("Trạng thái điểm danh: Vắng không phép\n");
                messageText.append("Lưu ý: Việc vắng mặt không phép có thể ảnh hưởng đến điểm chuyên cần của bạn.\n");
            }
        }

        // Thêm ghi chú nếu có
        if (attendance.getNotes() != null && !attendance.getNotes().isEmpty()) {
            messageText.append("\nGhi chú: ").append(attendance.getNotes()).append("\n");
        }

        messageText.append("\nNếu có thắc mắc về thông tin điểm danh, vui lòng liên hệ giáo viên hoặc quản trị viên hệ thống.\n\n");
        messageText.append("Trân trọng,\nBan quản lý đào tạo FSA");

        // Gửi email
        try {
            emailService.sendSimpleEmail(attendance.getStudent().getEmail(), subject, messageText.toString());
            logger.info("Đã gửi email thông báo điểm danh đến: {}", attendance.getStudent().getEmail());
        } catch (Exception e) {
            logger.error("Lỗi khi gửi email thông báo điểm danh: {}", e.getMessage(), e);
        }
    }

    /**
     * Gửi email nhắc nhở tùy chỉnh từ giảng viên đến sinh viên
     * @param studentEmail Email của sinh viên
     * @param studentName Tên của sinh viên
     * @param subject Tiêu đề email
     * @param customMessage Nội dung tùy chỉnh
     * @param teacher Thông tin giảng viên gửi nhắc nhở
     */
    @Async
    public void sendCustomReminderToStudent(String studentEmail, String studentName, String subject,
                                            String customMessage, User teacher) {
        String teacherName = teacher.getFirstName() + " " + teacher.getLastName();

        StringBuilder messageText = new StringBuilder();
        messageText.append("Xin chào ").append(studentName).append(",\n\n");
        messageText.append(customMessage).append("\n\n");
        messageText.append("Trân trọng,\n").append(teacherName);

        try {
            emailService.sendSimpleEmail(studentEmail, subject, messageText.toString());
            logger.info("Đã gửi email nhắc nhở từ {} đến: {}", teacherName, studentEmail);
        } catch (Exception e) {
            logger.error("Lỗi khi gửi email nhắc nhở: {}", e.getMessage(), e);
        }
    }

    /**
     * Gửi thông báo điểm danh hàng loạt cho nhiều sinh viên
     * @param attendances Danh sách bản ghi điểm danh
     */
    @Async
    public void sendBulkAttendanceNotifications(Iterable<Attendance> attendances) {
        for (Attendance attendance : attendances) {
            sendAttendanceStatusNotification(attendance);
        }
    }

    /**
     * Gửi email trực tiếp từ trang điểm danh với nội dung tùy chỉnh
     *
     * @param toEmail Email người nhận
     * @param student Thông tin sinh viên
     * @param subject Tiêu đề email
     * @param content Nội dung email (đã được xử lý thay thế biến)
     * @param sender Thông tin người gửi
     * @param attendance Thông tin điểm danh liên quan
     * @return true nếu gửi thành công, false nếu có lỗi
     */
    public boolean sendDirectEmail(String toEmail, User student, String subject, String content,
                                   User sender, Attendance attendance) {
        try {
            // Ghi log
            logger.info("Gửi email từ {} đến {} - Điểm danh ID: {}",
                    sender.getUsername(), student.getUsername(), attendance.getId());

            // Gửi email
            emailService.sendSimpleEmail(toEmail, subject, content);

            // Ghi log thành công
            logger.info("Đã gửi email thành công đến: {}", toEmail);
            return true;
        } catch (Exception e) {
            logger.error("Lỗi khi gửi email đến {}: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }
}