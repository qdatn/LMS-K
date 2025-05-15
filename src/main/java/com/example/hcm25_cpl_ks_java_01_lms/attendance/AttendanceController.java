package com.example.hcm25_cpl_ks_java_01_lms.attendance;

import com.example.hcm25_cpl_ks_java_01_lms.classes.Classes;
import com.example.hcm25_cpl_ks_java_01_lms.classes.ClassesService;
import com.example.hcm25_cpl_ks_java_01_lms.common.Constants;
import com.example.hcm25_cpl_ks_java_01_lms.user.User;
import com.example.hcm25_cpl_ks_java_01_lms.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/attendance")
@PreAuthorize("@customSecurityService.hasRoleForModule(authentication, 'Attendance')")
public class AttendanceController {
    private static final Logger logger = LoggerFactory.getLogger(AttendanceController.class);

    private final AttendanceService attendanceService;
    private final AttendanceStatisticService statisticService;
    private final ClassesService classesService;
    private final UserService userService;

    // Thêm AttendanceRepository để có thể lấy attendance sau khi cập nhật
    private final AttendanceRepository attendanceRepository;

    private final AttendanceImporter attendanceImporter;

    @Autowired
    public AttendanceController(AttendanceService attendanceService,
                                AttendanceStatisticService statisticService,
                                ClassesService classesService,
                                UserService userService,
                                AttendanceRepository attendanceRepository,
                                AttendanceImporter attendanceImporter) {
        this.attendanceService = attendanceService;
        this.statisticService = statisticService;
        this.classesService = classesService;
        this.userService = userService;
        this.attendanceRepository = attendanceRepository;
        this.attendanceImporter = attendanceImporter;
    }


    @GetMapping("/class/{classId}")
    public String listAttendanceDatesForClass(@PathVariable Long classId, Model model) {
        try {
            Classes classInfo = getAndValidateClass(classId);
            List<LocalDate> attendanceDates = attendanceService.getAttendanceDatesForClass(classId);
            double attendanceRate = statisticService.getAttendanceRateForClass(classId);
            List<AttendanceStatsDTO> dailyStats = statisticService.getDailyAttendanceStatsForClass(classId);

            model.addAttribute("classInfo", classInfo);
            model.addAttribute("attendanceDates", attendanceDates);
            model.addAttribute("attendanceRate", String.format("%.2f", attendanceRate));
            model.addAttribute("dailyStats", dailyStats);
            model.addAttribute("today", LocalDate.now());
            model.addAttribute("content", "attendance/dates");

            return Constants.LAYOUT;
        } catch (AttendanceException.ClassNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/attendance";
        } catch (Exception e) {
            logger.error("Error loading attendance data: {}", e.getMessage(), e);
            throw new AttendanceException.LoadingDataException("Failed to load attendance dates", e);
        }
    }

    @GetMapping("/class/{classId}/date/{date}")
    public String viewAttendanceForClassAndDate(
            @PathVariable Long classId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {
        try {
            Classes classInfo = getAndValidateClass(classId);
            List<LocalDate> attendanceDates = attendanceService.getAttendanceDatesForClass(classId);
            LocalDate today = LocalDate.now();
            List<LocalDate> pastAttendanceDates = attendanceDates.stream()
                    .filter(d -> !d.isAfter(today))
                    .collect(Collectors.toList());

            LocalDate latestAttendanceDate = pastAttendanceDates.isEmpty() ? null : pastAttendanceDates.get(0);
            Page<Attendance> attendanceRecords = attendanceService.getAttendanceByClassAndDate(classId, date, 0, Integer.MAX_VALUE);
            double attendanceRate = calculateAttendanceRate(attendanceRecords);

            model.addAttribute("classInfo", classInfo);
            model.addAttribute("date", date);
            model.addAttribute("latestAttendanceDate", latestAttendanceDate);
            model.addAttribute("attendanceRecords", attendanceRecords);
            model.addAttribute("attendanceRate", attendanceRate);
            model.addAttribute("today", today);
            model.addAttribute("content", "attendance/list");
            return Constants.LAYOUT;
        } catch (AttendanceException.ClassNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/attendance";
        } catch (Exception e) {
            logger.error("Error loading attendance data: {}", e.getMessage(), e);
            throw new AttendanceException.LoadingDataException("Failed to load attendance records", e);
        }
    }

    @GetMapping("/new")
    public String showCreateAttendanceForm(@RequestParam(required = false) Long classId, Model model) {
        try {
            List<Classes> activeClasses = attendanceService.getActiveClassesForAttendance();
            model.addAttribute("activeClasses", activeClasses);
            model.addAttribute("today", LocalDate.now());
            model.addAttribute("selectedClassId", classId);
            model.addAttribute("content", "attendance/create");
            return Constants.LAYOUT;
        } catch (Exception e) {
            logger.error("Error loading class list: {}", e.getMessage(), e);
            throw new AttendanceException.LoadingDataException("Failed to load active classes", e);
        }
    }

    @PostMapping("/create")
    public String createAttendance(
            @RequestParam Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            RedirectAttributes redirectAttributes) {
        try {
            attendanceService.createAttendanceForClass(classId, date);
            redirectAttributes.addFlashAttribute("success", "Attendance list created successfully!");
            return "redirect:/attendance/class/" + classId + "/date/" + date;
        } catch (Exception e) {
            logger.error("Error creating attendance list: {}", e.getMessage(), e);
            throw new AttendanceException.CreationException("Failed to create attendance list", e);
        }
    }

    @PostMapping("/mark")
    @ResponseBody
    public Map<String, Object> markAttendance(
            @RequestParam Long attendanceId,
            @RequestParam boolean isPresent,
            @RequestParam(required = false) Boolean isExcused) {
        try {
            // Gọi phương thức service để cập nhật trạng thái điểm danh
            attendanceService.markAttendance(attendanceId, isPresent, isExcused);

            // Lấy đối tượng attendance sau khi đã cập nhật
            Optional<Attendance> optionalAttendance = attendanceRepository.findById(attendanceId);

            if (!optionalAttendance.isPresent()) {
                throw new Exception("Không tìm thấy bản ghi điểm danh");
            }

            Attendance attendance = optionalAttendance.get();

            // Lấy thông tin lớp học và ngày để tính tỷ lệ tham dự
            Long classId = attendance.getClassInfo().getId();
            LocalDate date = attendance.getAttendanceDate();

            // Tính toán tỷ lệ tham dự mới
            Page<Attendance> attendanceRecords = attendanceService.getAttendanceByClassAndDate(
                    classId, date, 0, Integer.MAX_VALUE);
            double attendanceRate = calculateAttendanceRate(attendanceRecords);

            // Tạo response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("attendanceRate", String.format("%.2f", attendanceRate));

            // Nếu đánh dấu có mặt thì cập nhật giờ điểm danh
            if (attendance.getCheckedInTime() != null) {
                response.put("checkedInTime", attendance.getCheckedInTime().format(
                        DateTimeFormatter.ofPattern("HH:mm:ss")));
            } else if (isPresent) {
                // Nếu không có giờ điểm danh nhưng đánh dấu có mặt, tạo giờ hiện tại
                LocalTime now = LocalTime.now();
                response.put("checkedInTime", now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));

                // Cập nhật giờ điểm danh vào cơ sở dữ liệu
                attendance.setCheckedInTime(LocalDateTime.now());
                attendanceRepository.save(attendance);
            }

            return response;
        } catch (Exception e) {
            logger.error("Error marking attendance: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return response;
        }
    }

    @PostMapping("/notes")
    @ResponseBody
    public Map<String, Object> updateAttendanceNotes(@RequestParam Long attendanceId, @RequestParam String notes) {
        try {
            attendanceService.updateAttendanceNotes(attendanceId, notes);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            return response;
        } catch (Exception e) {
            logger.error("Error updating attendance notes: {}", e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return response;
        }
    }

    @PostMapping("/delete")
    public String deleteAttendance(@RequestParam Long attendanceId, RedirectAttributes redirectAttributes) {
        try {
            attendanceService.deleteAttendance(attendanceId);
            redirectAttributes.addFlashAttribute("success", "Bản ghi điểm danh đã được xóa thành công!");
            return "redirect:/attendance";
        } catch (Exception e) {
            logger.error("Error deleting attendance: {}", e.getMessage(), e);
            throw new AttendanceException.DeletionException("Failed to delete attendance record", e);
        }
    }

    @PostMapping("/delete-by-date")
    public String deleteAttendanceByDate(
            @RequestParam Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            RedirectAttributes redirectAttributes) {
        try {
            int count = attendanceService.deleteAttendanceForClassAndDate(classId, date);
            if (count > 0) {
                redirectAttributes.addFlashAttribute("success",
                        String.format("Đã xóa %d bản ghi điểm danh cho lớp vào ngày %s!", count, date));
            } else {
                redirectAttributes.addFlashAttribute("warning",
                        "Không tìm thấy bản ghi điểm danh nào để xóa.");
            }
            return "redirect:/attendance/class/" + classId;
        } catch (Exception e) {
            logger.error("Error deleting attendance by date: {}", e.getMessage(), e);
            throw new AttendanceException.DeletionException("Failed to delete attendance records by date", e);
        }
    }

    @PostMapping("/delete-date-range")
    public String deleteAttendanceByDateRange(
            @RequestParam Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            RedirectAttributes redirectAttributes) {
        try {
            int count = attendanceService.deleteAttendanceForClassInDateRange(classId, startDate, endDate);
            if (count > 0) {
                redirectAttributes.addFlashAttribute("success",
                        String.format("Đã xóa %d bản ghi điểm danh từ %s đến %s!", count, startDate, endDate));
            } else {
                redirectAttributes.addFlashAttribute("warning",
                        "Không tìm thấy bản ghi điểm danh nào để xóa trong khoảng thời gian đã chọn.");
            }
            return "redirect:/attendance/class/" + classId;
        } catch (Exception e) {
            logger.error("Error deleting attendance by date range: {}", e.getMessage(), e);
            throw new AttendanceException.DeletionException("Failed to delete attendance records by date range", e);
        }
    }

    // 1. Thay thế phương thức exportAttendanceToExcel hiện tại bằng phương thức sau:
    @GetMapping("/export/{classId}/{date}")
    public ResponseEntity<InputStreamResource> exportAttendanceToExcel(
            @PathVariable Long classId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            logger.info("Exporting attendance data for class ID: {} and date: {}", classId, date);

            // Kiểm tra và lấy thông tin lớp học
            Classes classInfo = getAndValidateClass(classId);
            if (classInfo == null) {
                logger.error("Class not found with ID: {}", classId);
                return ResponseEntity.notFound().build();
            }

            // Thực hiện export
            var excelStream = attendanceService.exportAttendanceToExcel(classId, date);

            // Thiết lập response
            String filename = "Attendance_" + sanitizeFilename(classInfo.getClassCode()) + "_" + date + ".xlsx";
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=" + filename);

            logger.info("Successfully exported attendance data for class ID: {} and date: {}", classId, date);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                    .body(new InputStreamResource(excelStream));
        } catch (AttendanceException e) {
            // Xử lý ngoại lệ AttendanceException
            logger.error("AttendanceException during export: {}", e.getMessage());
            if (e instanceof AttendanceException.ClassNotFoundException) {
                return ResponseEntity.notFound().build();
            }
            throw new AttendanceException.ExportException("Failed to export attendance data: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error exporting attendance data: {}", e.getMessage(), e);
            throw new AttendanceException.ExportException("Failed to export attendance data: " + e.getMessage(), e);
        }
    }

// 2. Thêm phương thức sanitizeFilename sau phương thức calculateAttendanceRate
    /**
     * Sanitize filename to prevent invalid characters
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "export";
        }
        // Replace characters that are not allowed in filenames
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    @GetMapping("/student/{studentId}/class/{classId}")
    public String viewStudentAttendance(@PathVariable Long studentId, @PathVariable Long classId, Model model) {
        try {
            User student = userService.getUserById(studentId);
            if (student == null) {
                throw new AttendanceException.StudentNotFoundException(studentId);
            }

            Classes classInfo = getAndValidateClass(classId);
            double attendanceRate = statisticService.getAttendanceRateForStudent(studentId, classId);

            model.addAttribute("student", student);
            model.addAttribute("classInfo", classInfo);
            model.addAttribute("attendanceRate", String.format("%.2f", attendanceRate));
            model.addAttribute("content", "attendance/student-attendance");
            return Constants.LAYOUT;
        } catch (AttendanceException.StudentNotFoundException | AttendanceException.ClassNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/classes/" + classId + "/students";
        } catch (Exception e) {
            logger.error("Error loading student attendance data: {}", e.getMessage(), e);
            throw new AttendanceException.LoadingDataException("Failed to load student attendance data", e);
        }
    }

    @PostMapping("/bulk-create")
    public String createBulkAttendance(
            @RequestParam Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "false") boolean weekendsOnly,
            @RequestParam(required = false, defaultValue = "false") boolean weekdaysOnly,
            RedirectAttributes redirectAttributes) {
        try {
            int createdCount = attendanceService.createBulkAttendanceForClass(
                    classId, startDate, endDate, weekendsOnly, weekdaysOnly);

            redirectAttributes.addFlashAttribute("success",
                    "Created " + createdCount + " attendance records from " +
                            startDate.toString() + " to " + endDate.toString());

            return "redirect:/attendance/class/" + classId;
        } catch (Exception e) {
            logger.error("Error creating bulk attendance records: {}", e.getMessage(), e);
            throw new AttendanceException.CreationException("Failed to create bulk attendance records", e);
        }
    }

    private Classes getAndValidateClass(Long classId) {
        Classes classInfo = classesService.getClassById(classId);
        if (classInfo == null) {
            throw new AttendanceException.ClassNotFoundException(classId);
        }
        return classInfo;
    }

    private double calculateAttendanceRate(Page<Attendance> attendanceRecords) {
        if (attendanceRecords == null || attendanceRecords.getTotalElements() == 0) {
            return 0.0;
        }

        long presentCount = attendanceRecords.getContent().stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsPresent()))
                .count();

        return (double) presentCount * 100.0 / attendanceRecords.getTotalElements();
    }

    /**
     * Endpoint để tải template Excel cho import điểm danh
     */
    @GetMapping("/import-template/{classId}/{date}")
    public ResponseEntity<InputStreamResource> getImportTemplate(
            @PathVariable Long classId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            Classes classInfo = getAndValidateClass(classId);
            var templateStream = attendanceImporter.generateImportTemplate(classId, date);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=AttendanceTemplate_" +
                    classInfo.getClassCode() + "_" + date + ".xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                    .body(new InputStreamResource(templateStream));
        } catch (AttendanceException.ClassNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Lỗi khi tạo template import điểm danh: {}", e.getMessage(), e);
            throw new AttendanceException.ExportException("Không thể tạo template điểm danh", e);
        }
    }

    /**
     * Endpoint để import điểm danh từ file Excel
     */
    @PostMapping("/import")
    @ResponseBody
    public Map<String, Object> importAttendance(
            @RequestParam Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Kiểm tra file
            if (file.isEmpty()) {
                response.put("status", "error");
                response.put("message", "Vui lòng chọn file Excel để import");
                return response;
            }

            // Kiểm tra định dạng file
            String contentType = file.getContentType();
            if (contentType == null ||
                    (!contentType.equals("application/vnd.ms-excel") &&
                            !contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))) {
                response.put("status", "error");
                response.put("message", "Chỉ chấp nhận file Excel (.xls, .xlsx)");
                return response;
            }

            // Xác thực thông tin lớp học
            getAndValidateClass(classId);

            // Thực hiện import
            int recordsImported = attendanceImporter.importAttendanceFromExcel(classId, date, file.getInputStream());

            // Tính toán tỷ lệ tham dự mới
            Page<Attendance> attendanceRecords = attendanceService.getAttendanceByClassAndDate(
                    classId, date, 0, Integer.MAX_VALUE);
            double attendanceRate = calculateAttendanceRate(attendanceRecords);

            // Trả về kết quả thành công
            response.put("status", "success");
            response.put("message", "Đã import thành công " + recordsImported + " bản ghi điểm danh");
            response.put("recordsImported", recordsImported);
            response.put("attendanceRate", String.format("%.2f", attendanceRate));

            return response;
        } catch (AttendanceException.ClassNotFoundException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return response;
        } catch (Exception e) {
            logger.error("Lỗi khi import điểm danh: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Lỗi khi import điểm danh: " + e.getMessage());
            return response;
        }
    }
}