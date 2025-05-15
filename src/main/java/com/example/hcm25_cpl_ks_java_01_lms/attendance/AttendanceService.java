package com.example.hcm25_cpl_ks_java_01_lms.attendance;

import com.example.hcm25_cpl_ks_java_01_lms.classes.Classes;
import com.example.hcm25_cpl_ks_java_01_lms.classes.ClassesRepository;
import com.example.hcm25_cpl_ks_java_01_lms.user.User;
import com.example.hcm25_cpl_ks_java_01_lms.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AttendanceService {
    private static final Logger logger = LoggerFactory.getLogger(AttendanceService.class);

    private final AttendanceRepository attendanceRepository;
    private final ClassesRepository classesRepository;
    private final UserRepository userRepository;
    private final AttendanceExcelExporter attendanceExcelExporter;
    private final EmailAttendanceService emailAttendanceService;

    @Autowired
    public AttendanceService(AttendanceRepository attendanceRepository,
                             ClassesRepository classesRepository,
                             UserRepository userRepository,
                             AttendanceExcelExporter attendanceExcelExporter, EmailAttendanceService emailAttendanceService) {
        this.attendanceRepository = attendanceRepository;
        this.classesRepository = classesRepository;
        this.userRepository = userRepository;
        this.attendanceExcelExporter = attendanceExcelExporter;
        this.emailAttendanceService = emailAttendanceService;
    }

    public Page<Attendance> getAttendanceByClassAndDate(Long classId, LocalDate date, int page, int size) {
        Classes classInfo = findClassById(classId);
        return attendanceRepository.findByClassInfoAndAttendanceDateOrderByStudent_Username(
                classInfo, date, PageRequest.of(page, size));
    }

    @Transactional
    public void createAttendanceForClass(Long classId, LocalDate date) {
        Classes classInfo = findClassById(classId);
        validateActiveClass(classInfo);

        List<User> students = classInfo.getStudents();
        if (students.isEmpty()) {
            throw new AttendanceException("No students in this class");
        }

        for (User student : students) {
            if (attendanceRepository.findByClassInfoAndStudentAndAttendanceDate(classInfo, student, date).isEmpty()) {
                Attendance attendance = new Attendance();
                attendance.setClassInfo(classInfo);
                attendance.setStudent(student);
                attendance.setAttendanceDate(date);
                attendance.setIsPresent(false);
                attendance.setIsExcused(false);
                attendanceRepository.save(attendance);
            }
        }
    }



    /**
     * Phương thức tự động tạo ngày điểm danh khi tạo lớp học mới
     * Chỉ tạo các ngày, không cần sinh viên
     */
    @Transactional
    public int initAttendanceDatesForNewClass(Long classId, LocalDate startDate, LocalDate endDate) {
        Classes classInfo = findClassById(classId);
        validateActiveClass(classInfo);

        if (startDate.isAfter(endDate)) {
            throw new AttendanceException("Start date cannot be after end date");
        }

        List<LocalDate> datesToCreate = generateDateList(startDate, endDate, false, false);
        int createdCount = 0;

        for (LocalDate date : datesToCreate) {
            // Kiểm tra xem ngày này đã có attendance chưa
            boolean exists = !attendanceRepository.findByClassInfoAndAttendanceDate(classInfo, date).isEmpty();

            if (!exists) {
                Attendance attendance = new Attendance();
                attendance.setClassInfo(classInfo);
                attendance.setStudent(null);  // Student là null vì đây là tạo trước khi có sinh viên
                attendance.setAttendanceDate(date);
                attendance.setIsPresent(false);
                attendance.setIsExcused(false);
                attendanceRepository.save(attendance);
                createdCount++;
            }
        }

        logger.info("Created {} attendance dates for new class ID: {}", createdCount, classId);
        return createdCount;
    }

    /**
     * Đánh dấu trạng thái điểm danh của sinh viên
     * @param attendanceId ID bản ghi điểm danh
     * @param isPresent Trạng thái có mặt
     * @param isExcused Trạng thái có phép (nếu vắng mặt)
     */
    @Transactional
    public void markAttendance(Long attendanceId, boolean isPresent, Boolean isExcused) {
        Attendance attendance = findAttendanceById(attendanceId);

        // Lưu trạng thái cũ để so sánh
        boolean wasPresent = attendance.getIsPresent() != null && attendance.getIsPresent();
        boolean wasExcused = attendance.getIsExcused() != null && attendance.getIsExcused();

        // Cập nhật trạng thái mới
        attendance.setIsPresent(isPresent);
        attendance.setCheckedInTime(isPresent ? LocalDateTime.now() : null);

        if (isExcused != null) {
            attendance.setIsExcused(isExcused);
        }

        attendanceRepository.save(attendance);

        // Kiểm tra xem có thay đổi trạng thái không
        boolean statusChanged = (wasPresent != isPresent) || (wasExcused != (isExcused != null && isExcused));

        // Nếu có thay đổi trạng thái và sinh viên tồn tại, gửi thông báo
        if (statusChanged && attendance.getStudent() != null) {
            logger.info("Gửi email thông báo trạng thái điểm danh cho sinh viên: {}",
                    attendance.getStudent().getEmail());
            emailAttendanceService.sendAttendanceStatusNotification(attendance);
        }
    }

    @Transactional
    public void updateAttendanceNotes(Long attendanceId, String notes) {
        Attendance attendance = findAttendanceById(attendanceId);
        attendance.setNotes(notes);
        attendanceRepository.save(attendance);
    }

    public List<LocalDate> getAttendanceDatesForClass(Long classId) {
        validateClassExists(classId);
        return attendanceRepository.findDistinctAttendanceDatesByClassId(classId);
    }

    public List<Classes> getActiveClassesForAttendance() {
        try {
            logger.info("Attempting to fetch active classes");
            List<Classes> classes = classesRepository.findByIsActiveTrue();
            logger.info("Found {} active classes", classes.size());
            return classes;
        } catch (Exception e) {
            logger.error("Error fetching active classes: {}", e.getMessage(), e);

            return new ArrayList<>();
        }
    }

    // Thay thế phương thức exportAttendanceToExcel hiện tại bằng phương thức sau:
    public ByteArrayInputStream exportAttendanceToExcel(Long classId, LocalDate date) throws IOException {
        logger.info("Preparing to export attendance data for class ID: {} and date: {}", classId, date);

        try {
            // Kiểm tra và lấy thông tin lớp học
            Classes classInfo = findClassById(classId);
            if (classInfo == null) {
                logger.error("Class not found with ID: {}", classId);
                throw new AttendanceException("Class not found with ID: " + classId);
            }

            // Lấy danh sách điểm danh
            List<Attendance> attendanceList = attendanceRepository.findByClassInfoAndAttendanceDate(classInfo, date);

            // Kiểm tra danh sách có dữ liệu
            if (attendanceList == null || attendanceList.isEmpty()) {
                logger.warn("No attendance records found for class ID: {} and date: {}", classId, date);
                throw new AttendanceException("No attendance records found for this class on the specified date");
            }

            // Kiểm tra dữ liệu hợp lệ trước khi export
            validateAttendanceData(attendanceList);

            // Thực hiện export
            logger.info("Exporting {} attendance records to Excel", attendanceList.size());
            return attendanceExcelExporter.exportToExcel(attendanceList, date, classInfo.getName());
        } catch (Exception e) {
            logger.error("Error exporting attendance data: {}", e.getMessage(), e);
            if (e instanceof AttendanceException) {
                throw (AttendanceException) e;
            }
            throw new AttendanceException("Failed to export attendance data: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra dữ liệu điểm danh trước khi export
     */
    private void validateAttendanceData(List<Attendance> attendanceList) {
        if (attendanceList == null) {
            logger.error("Attendance list is null");
            throw new AttendanceException("Attendance list cannot be null");
        }

        // Kiểm tra từng bản ghi
        for (int i = 0; i < attendanceList.size(); i++) {
            Attendance attendance = attendanceList.get(i);

            if (attendance == null) {
                logger.warn("Null attendance record at index {}", i);
                continue;
            }

            if (attendance.getStudent() == null) {
                logger.warn("Attendance record at index {} has null student", i);
                // Không ném ngoại lệ, chỉ ghi log
            }

            // Kiểm tra dữ liệu khác (nếu cần)
            if (attendance.getAttendanceDate() == null) {
                logger.warn("Attendance record at index {} has null date", i);
            }
        }

        logger.info("Attendance data validation completed for {} records", attendanceList.size());
    }

    @Transactional
    public int createBulkAttendanceForClass(Long classId, LocalDate startDate, LocalDate endDate,
                                            boolean weekendsOnly, boolean weekdaysOnly) {
        Classes classInfo = findClassById(classId);
        validateActiveClass(classInfo);

        if (startDate.isAfter(endDate)) {
            throw new AttendanceException("Start date cannot be after end date");
        }

        List<LocalDate> datesToCreate = generateDateList(startDate, endDate, weekendsOnly, weekdaysOnly);
        int createdCount = 0;

        for (LocalDate date : datesToCreate) {
            try {
                createAttendanceForClass(classId, date);
                createdCount++;
            } catch (Exception e) {
                logger.error("Error creating attendance for date {}: {}", date, e.getMessage());
            }
        }

        return createdCount;
    }

    @Transactional
    public void deleteAttendance(Long attendanceId) {
        Attendance attendance = findAttendanceById(attendanceId);
        attendanceRepository.delete(attendance);
        logger.info("Deleted attendance record with ID: {}", attendanceId);
    }

    @Transactional
    public int deleteAttendanceForClassAndDate(Long classId, LocalDate date) {
        Classes classInfo = findClassById(classId);
        List<Attendance> attendanceList = attendanceRepository.findByClassInfoAndAttendanceDate(classInfo, date);

        if (attendanceList.isEmpty()) {
            logger.warn("No attendance records found for class ID {} on date {}", classId, date);
            return 0;
        }

        int count = attendanceList.size();
        attendanceRepository.deleteAll(attendanceList);
        logger.info("Deleted {} attendance records for class ID {} on date {}", count, classId, date);
        return count;
    }

    @Transactional
    public int deleteAttendanceForClassInDateRange(Long classId, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new AttendanceException("Start date cannot be after end date");
        }

        Classes classInfo = findClassById(classId);
        List<Attendance> attendanceToDelete = new ArrayList<>();
        int totalDeleted = 0;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<Attendance> dailyAttendance = attendanceRepository.findByClassInfoAndAttendanceDate(classInfo, date);
            attendanceToDelete.addAll(dailyAttendance);
            totalDeleted += dailyAttendance.size();
        }

        if (!attendanceToDelete.isEmpty()) {
            attendanceRepository.deleteAll(attendanceToDelete);
            logger.info("Deleted {} attendance records for class ID {} from {} to {}",
                    totalDeleted, classId, startDate, endDate);
        } else {
            logger.warn("No attendance records found for class ID {} from {} to {}",
                    classId, startDate, endDate);
        }

        return totalDeleted;
    }

    // Helper methods made public so they can be used by AttendanceStatisticService
    public Classes findClassById(Long classId) {
        return classesRepository.findById(classId)
                .orElseThrow(() -> new AttendanceException("Class not found with ID: " + classId));
    }

    public Attendance findAttendanceById(Long attendanceId) {
        return attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new AttendanceException("Attendance record not found with ID: " + attendanceId));
    }

    public void validateClassExists(Long classId) {
        if (!classesRepository.existsById(classId)) {
            throw new AttendanceException("Class not found with ID: " + classId);
        }
    }

    public void validateStudentExists(Long studentId) {
        if (!userRepository.existsById(studentId)) {
            throw new AttendanceException("Student not found with ID: " + studentId);
        }
    }

    public void validateActiveClass(Classes classInfo) {
        if (!classInfo.getIsActive()) {
            throw new AttendanceException("Cannot create attendance for inactive class");
        }
    }

    private List<LocalDate> generateDateList(LocalDate startDate, LocalDate endDate,
                                             boolean weekendsOnly, boolean weekdaysOnly) {
        List<LocalDate> dates = new ArrayList<>();
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        for (int i = 0; i < daysBetween; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            boolean isWeekend = currentDate.getDayOfWeek() == DayOfWeek.SATURDAY
                    || currentDate.getDayOfWeek() == DayOfWeek.SUNDAY;

            if ((weekendsOnly && !isWeekend) || (weekdaysOnly && isWeekend)) {
                continue;
            }

            dates.add(currentDate);
        }

        return dates;
    }
}