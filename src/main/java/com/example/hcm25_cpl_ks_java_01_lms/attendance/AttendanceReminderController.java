package com.example.hcm25_cpl_ks_java_01_lms.attendance;

import com.example.hcm25_cpl_ks_java_01_lms.classes.Classes;
import com.example.hcm25_cpl_ks_java_01_lms.classes.ClassesService;
import com.example.hcm25_cpl_ks_java_01_lms.common.Constants;
import com.example.hcm25_cpl_ks_java_01_lms.user.User;
import com.example.hcm25_cpl_ks_java_01_lms.user.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/attendance/reminder")
@PreAuthorize("@customSecurityService.hasRoleForModule(authentication, 'Attendance')")
public class AttendanceReminderController {
    private static final Logger logger = LoggerFactory.getLogger(AttendanceReminderController.class);

    private final ClassesService classesService;
    private final UserService userService;
    private final EmailAttendanceService emailAttendanceService;
    private final AttendanceRepository attendanceRepository;

    @Value("${jwt.secret:defaultSecretKey}")
    private String jwtSecret;

    @Autowired
    public AttendanceReminderController(
            ClassesService classesService,
            UserService userService,
            EmailAttendanceService emailAttendanceService,
            AttendanceRepository attendanceRepository) {
        this.classesService = classesService;
        this.userService = userService;
        this.emailAttendanceService = emailAttendanceService;
        this.attendanceRepository = attendanceRepository;
    }

    /**
     * Hiển thị form gửi nhắc nhở cho các sinh viên trong lớp
     */
    @GetMapping("/class/{classId}")
    public String showClassReminderForm(@PathVariable Long classId, Model model) {
        try {
            Classes classInfo = classesService.getClassById(classId);
            if (classInfo == null) {
                throw new AttendanceException.ClassNotFoundException(classId);
            }

            // Lấy thông tin giảng viên từ JWT token
            User currentUser = getCurrentUserFromJwt();

            model.addAttribute("classInfo", classInfo);
            model.addAttribute("students", classInfo.getStudents());
            model.addAttribute("teacher", currentUser);
            model.addAttribute("content", "attendance/reminder-form");
            return Constants.LAYOUT;
        } catch (Exception e) {
            logger.error("Lỗi khi tải form nhắc nhở: {}", e.getMessage(), e);
            throw new AttendanceException.LoadingDataException("Không thể tải form nhắc nhở", e);
        }
    }

    /**
     * Hiển thị form gửi nhắc nhở cho một sinh viên cụ thể
     */
    @GetMapping("/student/{studentId}")
    public String showStudentReminderForm(@PathVariable Long studentId,
                                          @RequestParam(required = false) Long classId,
                                          Model model) {
        try {
            User student = userService.getUserById(studentId);
            if (student == null) {
                throw new AttendanceException.StudentNotFoundException(studentId);
            }

            Classes classInfo = null;
            if (classId != null) {
                classInfo = classesService.getClassById(classId);
            }

            // Lấy thông tin giảng viên từ JWT token
            User currentUser = getCurrentUserFromJwt();

            model.addAttribute("student", student);
            model.addAttribute("classInfo", classInfo);
            model.addAttribute("teacher", currentUser);
            model.addAttribute("content", "attendance/reminder-form-single");
            return Constants.LAYOUT;
        } catch (Exception e) {
            logger.error("Lỗi khi tải form nhắc nhở sinh viên: {}", e.getMessage(), e);
            throw new AttendanceException.LoadingDataException("Không thể tải form nhắc nhở sinh viên", e);
        }
    }

    /**
     * Xử lý gửi nhắc nhở cho một sinh viên
     */
    @PostMapping("/send-to-student")
    public String sendReminderToStudent(
            @RequestParam Long studentId,
            @RequestParam(required = false) Long classId,
            @RequestParam String subject,
            @RequestParam String message,
            RedirectAttributes redirectAttributes) {
        try {
            User student = userService.getUserById(studentId);
            if (student == null) {
                throw new AttendanceException.StudentNotFoundException(studentId);
            }

            // Lấy thông tin giảng viên từ JWT token
            User teacher = getCurrentUserFromJwt();

            // Gửi email
            emailAttendanceService.sendCustomReminderToStudent(
                    student.getEmail(),
                    student.getFirstName() + " " + student.getLastName(),
                    subject,
                    message,
                    teacher);

            redirectAttributes.addFlashAttribute("success", "Đã gửi nhắc nhở đến sinh viên " +
                    student.getFirstName() + " " + student.getLastName());

            if (classId != null) {
                return "redirect:/attendance/class/" + classId + "/date/" + LocalDate.now();
            } else {
                return "redirect:/attendance";
            }
        } catch (Exception e) {
            logger.error("Lỗi khi gửi nhắc nhở: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Lỗi khi gửi nhắc nhở: " + e.getMessage());
            return "redirect:/attendance";
        }
    }

    /**
     * Xử lý gửi nhắc nhở cho nhiều sinh viên trong lớp
     */
    /**
     * Xử lý gửi nhắc nhở cho nhiều sinh viên trong lớp
     */
    @PostMapping("/send-to-class")
    @ResponseBody
    public Map<String, Object> sendReminderToClass(
            @RequestBody Map<String, Object> requestData) {
        Map<String, Object> response = new HashMap<>();

        try {
            Long classId = Long.valueOf(requestData.get("classId").toString());

            // Xử lý chuyển đổi danh sách studentIds một cách an toàn
            List<?> rawStudentIds = (List<?>) requestData.get("studentIds");
            List<Long> studentIds = new ArrayList<>();

            // Chuyển đổi từng phần tử sang Long
            if (rawStudentIds != null) {
                for (Object id : rawStudentIds) {
                    if (id instanceof String) {
                        studentIds.add(Long.valueOf((String) id));
                    } else if (id instanceof Number) {
                        studentIds.add(((Number) id).longValue());
                    } else if (id != null) {
                        studentIds.add(Long.valueOf(id.toString()));
                    }
                }
            }

            String subject = (String) requestData.get("subject");
            String message = (String) requestData.get("message");

            Classes classInfo = classesService.getClassById(classId);
            if (classInfo == null) {
                throw new AttendanceException.ClassNotFoundException(classId);
            }

            // Lấy thông tin giảng viên từ JWT token
            User teacher = getCurrentUserFromJwt();

            int sentCount = 0;
            for (Long studentId : studentIds) {
                User student = userService.getUserById(studentId);
                if (student != null && student.getEmail() != null) {
                    // Xử lý các biến trong nội dung
                    String studentName = student.getFirstName() + " " + student.getLastName();
                    String processedMessage = message
                            .replace("{studentName}", studentName)
                            .replace("{className}", classInfo.getName())
                            .replace("{classCode}", classInfo.getClassCode())
                            .replace("{date}", LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));

                    // Gửi email
                    emailAttendanceService.sendCustomReminderToStudent(
                            student.getEmail(),
                            studentName,
                            subject,
                            processedMessage,
                            teacher);
                    sentCount++;
                }
            }

            response.put("status", "success");
            response.put("message", "Đã gửi nhắc nhở đến " + sentCount + " sinh viên");
            return response;
        } catch (Exception e) {
            logger.error("Lỗi khi gửi nhắc nhở đến lớp: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Lỗi khi gửi nhắc nhở: " + e.getMessage());
            return response;
        }
    }


    /**
     * Gửi thông báo điểm danh cho tất cả sinh viên vắng mặt trong lớp vào một ngày cụ thể
     */
    @PostMapping("/notify-absent-students")
    public ResponseEntity<?> notifyAbsentStudents(
            @RequestParam Long classId,
            @RequestParam String date) {
        try {

            Classes classInfo = classesService.getClassById(classId);
            if (classInfo == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy lớp học"));
            }

            LocalDate attendanceDate = LocalDate.parse(date);

            // Lấy danh sách sinh viên vắng mặt
            List<Attendance> absentStudents = attendanceRepository.findByClassInfoAndAttendanceDateAndIsPresent(
                    classInfo, attendanceDate, false);

            if (absentStudents.isEmpty()) {
                return ResponseEntity.ok(Map.of("message", "Không có sinh viên vắng mặt trong ngày này"));
            }

            // Gửi thông báo
            emailAttendanceService.sendBulkAttendanceNotifications(absentStudents);

            return ResponseEntity.ok(Map.of(
                    "message", "Đã gửi " + absentStudents.size() + " thông báo vắng mặt",
                    "count", absentStudents.size()
            ));
        } catch (Exception e) {
            logger.error("Lỗi khi gửi thông báo vắng mặt: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi khi gửi thông báo: " + e.getMessage()));
        }
    }

    /**
     * Phương thức để lấy thông tin người dùng hiện tại từ JWT token
     */
    private User getCurrentUserFromJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Optional<User> userOptional = userService.findByUsername(username);
        if (!userOptional.isPresent()) {
            logger.error("Không tìm thấy thông tin người dùng từ JWT với username: {}", username);
            throw new RuntimeException("Không tìm thấy thông tin người dùng");
        }

        return userOptional.get();
    }

}