package com.example.hcm25_cpl_ks_java_01_lms.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO tổng hợp để lưu trữ các loại thống kê điểm danh khác nhau.
 * Được sử dụng cho thống kê theo ngày, theo lớp, theo tháng hoặc theo năm.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceStatsDTO {
    private Integer year;
    private Integer month;
    private Integer weekOfMonth;
    private String dayOfWeek;
    private LocalDate date;
    private String statsType;

    private Long classId;
    private String className;
    private String classCode;

    private int totalAttendances;
    private int totalStudents;
    private int presentCount;
    private int presentStudents;
    private int absentCount;
    private int excusedCount;
    private double presentRate;
    private double absentRate;
    private double excusedRate;
    private double attendanceRate;
    private int studentCount;


    public static AttendanceStatsDTO createClassStats(
            Long classId, String className, String classCode, 
            int totalRecords, int presentCount, double attendanceRate, int studentCount) {
        AttendanceStatsDTO dto = new AttendanceStatsDTO();
        dto.setStatsType("CLASS");
        dto.setClassId(classId);
        dto.setClassName(className);
        dto.setClassCode(classCode);
        dto.setTotalAttendances(totalRecords);
        dto.setPresentCount(presentCount);
        dto.setAttendanceRate(attendanceRate);
        dto.setPresentRate(attendanceRate);
        dto.setStudentCount(studentCount);
        return dto;
    }

    public static AttendanceStatsDTO createDailyStats(
            LocalDate date, int totalStudents, int presentStudents, double attendanceRate) {
        AttendanceStatsDTO dto = new AttendanceStatsDTO();
        dto.setStatsType("DAY");
        dto.setDate(date);
        dto.setTotalStudents(totalStudents);
        dto.setPresentStudents(presentStudents);
        dto.setPresentCount(presentStudents);
        dto.setAttendanceRate(attendanceRate);
        dto.setPresentRate(attendanceRate);
        return dto;
    }

}