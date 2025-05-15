package com.example.hcm25_cpl_ks_java_01_lms.attendance;

import com.example.hcm25_cpl_ks_java_01_lms.classes.Classes;
import com.example.hcm25_cpl_ks_java_01_lms.classes.ClassesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class AttendanceStatisticService {
    private final AttendanceRepository attendanceRepository;
    private final ClassesRepository classesRepository;
    private final AttendanceService attendanceService;

    @Autowired
    public AttendanceStatisticService(
            AttendanceRepository attendanceRepository,
            ClassesRepository classesRepository,
            AttendanceService attendanceService) {
        this.attendanceRepository = attendanceRepository;
        this.classesRepository = classesRepository;
        this.attendanceService = attendanceService;
    }

    public double getAttendanceRateForClass(Long classId) {
        attendanceService.validateClassExists(classId);
        int totalAttendances = attendanceRepository.countTotalAttendancesForClassUntilToday(classId);
        int presentAttendances = attendanceRepository.countPresentAttendancesForClassUntilToday(classId);
        return calculateRate(presentAttendances, totalAttendances);
    }

    public double getAttendanceRateForStudent(Long studentId, Long classId) {
        attendanceService.validateClassExists(classId);
        attendanceService.validateStudentExists(studentId);

        int totalDays = attendanceRepository.countTotalDaysForStudentInClass(studentId, classId);
        int presentDays = attendanceRepository.countPresentDaysForStudentInClass(studentId, classId);
        return calculateRate(presentDays, totalDays);
    }

    public int getTotalAttendanceDates() {
        return attendanceRepository.countDistinctAttendanceDates();
    }

    public double getAverageAttendanceRate() {
        List<Object[]> results = attendanceRepository.countAttendanceAndPresentByYearAndMonthUntilToday();
        int totalAttendances = 0, totalPresent = 0;

        for (Object[] result : results) {
            totalAttendances += ((Number) result[2]).intValue();
            totalPresent += ((Number) result[3]).intValue();
        }

        return calculateRate(totalPresent, totalAttendances);
    }

    public List<AttendanceStatsDTO> getDailyAttendanceStatsForClass(Long classId) {
        attendanceService.validateClassExists(classId);

        // Lấy thông tin lớp để biết số sinh viên thực sự
        Classes classInfo = classesRepository.findById(classId)
                .orElseThrow(() -> new AttendanceException("Class not found with ID: " + classId));

        List<LocalDate> dates = attendanceService.getAttendanceDatesForClass(classId);
        List<AttendanceStatsDTO> result = new ArrayList<>();

        // Sử dụng số lượng sinh viên thực từ lớp học
        int realStudentCount = classInfo.getStudents().size();

        for (LocalDate date : dates) {
            // Sử dụng số sinh viên thực, không đếm từ attendance
            int totalStudents = realStudentCount;

            // Vẫn giữ nguyên cách đếm sinh viên có mặt
            int presentStudents = attendanceRepository.countPresentStudentsForClassAndDate(classId, date);
            double rate = calculateRate(presentStudents, totalStudents);

            AttendanceStatsDTO stats = AttendanceStatsDTO.createDailyStats(
                    date, totalStudents, presentStudents, rate);
            result.add(stats);
        }

        return result;
    }

    public Map<Long, AttendanceStatsDTO> getClassAttendanceStats() {
        List<Object[]> classStats = attendanceRepository.findClassesWithAttendanceRatesSorted();
        Map<Long, AttendanceStatsDTO> resultMap = new HashMap<>();

        for (Object[] stat : classStats) {
            Long classId = ((Number) stat[0]).longValue();
            Classes classInfo = classesRepository.findById(classId).orElse(null);

            if (classInfo != null) {
                int totalRecords = ((Number) stat[3]).intValue();
                int presentCount = ((Number) stat[4]).intValue();
                double rate = calculateRate(presentCount, totalRecords);
                AttendanceStatsDTO stats = AttendanceStatsDTO.createClassStats(
                        classId,
                        (String) stat[1],
                        (String) stat[2],
                        totalRecords,
                        presentCount,
                        rate,
                        classInfo.getStudents().size()
                );
                resultMap.put(classId, stats);
            }
        }

        return resultMap;
    }

    public Map<String, Double> getAttendanceStatusStats() {
        Map<String, Double> stats = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate startDate = LocalDate.of(2000, 1, 1);
        long totalRecords = attendanceRepository.countByAttendanceDateBetween(startDate, today);

        if (totalRecords == 0) {
            stats.put("present", 0.0);
            stats.put("absent", 0.0);
            stats.put("excused", 0.0);
            return stats;
        }

        long presentCount = attendanceRepository.countByIsPresentAndAttendanceDateBetween(
                true, startDate, today);
        long absentCount = attendanceRepository.countByIsPresentAndIsExcusedAndAttendanceDateBetween(
                false, false, startDate, today);
        long excusedCount = attendanceRepository.countByIsPresentAndIsExcusedAndAttendanceDateBetween(
                false, true, startDate, today);

        stats.put("present", (double) presentCount * 100 / totalRecords);
        stats.put("absent", (double) absentCount * 100 / totalRecords);
        stats.put("excused", (double) excusedCount * 100 / totalRecords);

        return stats;
    }

    private double calculateRate(int numerator, int denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / denominator * 100;
    }
}