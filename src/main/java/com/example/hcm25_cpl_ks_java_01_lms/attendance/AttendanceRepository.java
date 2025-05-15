package com.example.hcm25_cpl_ks_java_01_lms.attendance;

import com.example.hcm25_cpl_ks_java_01_lms.classes.Classes;
import com.example.hcm25_cpl_ks_java_01_lms.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByClassInfoAndAttendanceDate(Classes classInfo, LocalDate date);

    Optional<Attendance> findByClassInfoAndStudentAndAttendanceDate(Classes classInfo, User student, LocalDate date);

    @Query("SELECT a FROM Attendance a WHERE a.classInfo = :classInfo AND a.attendanceDate = :date ORDER BY a.student.username")
    Page<Attendance> findByClassInfoAndAttendanceDateOrderByStudent_Username(
            @Param("classInfo") Classes classInfo,
            @Param("date") LocalDate date,
            Pageable pageable);

    @Query("SELECT DISTINCT a.attendanceDate FROM Attendance a WHERE a.classInfo.id = :classId ORDER BY a.attendanceDate DESC")
    List<LocalDate> findDistinctAttendanceDatesByClassId(@Param("classId") Long classId);

    @Query("SELECT COUNT(DISTINCT a.attendanceDate) FROM Attendance a")
    int countDistinctAttendanceDates();

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.classInfo.id = :classId AND a.attendanceDate = :date AND a.isPresent = true")
    int countPresentStudentsForClassAndDate(@Param("classId") Long classId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.classInfo.id = :classId AND a.attendanceDate = :date")
    int countTotalStudentsForClassAndDate(@Param("classId") Long classId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND a.classInfo.id = :classId AND a.isPresent = true")
    int countPresentDaysForStudentInClass(@Param("studentId") Long studentId, @Param("classId") Long classId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND a.classInfo.id = :classId")
    int countTotalDaysForStudentInClass(@Param("studentId") Long studentId, @Param("classId") Long classId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.classInfo.id = :classId AND a.attendanceDate <= CURRENT_DATE")
    int countTotalAttendancesForClassUntilToday(@Param("classId") Long classId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.classInfo.id = :classId AND a.isPresent = true AND a.attendanceDate <= CURRENT_DATE")
    int countPresentAttendancesForClassUntilToday(@Param("classId") Long classId);

    long countByAttendanceDateBetween(LocalDate startDate, LocalDate endDate);

    long countByIsPresentAndAttendanceDateBetween(boolean isPresent, LocalDate startDate, LocalDate endDate);

    long countByIsPresentAndIsExcusedAndAttendanceDateBetween(boolean isPresent, boolean isExcused, LocalDate startDate, LocalDate endDate);

    @Query("SELECT c.id, c.name, c.classCode, COUNT(a), COUNT(CASE WHEN a.isPresent = true THEN 1 ELSE null END) " +
            "FROM Attendance a JOIN a.classInfo c GROUP BY c.id, c.name, c.classCode " +
            "ORDER BY CASE WHEN COUNT(a) = 0 THEN 0 ELSE " +
            "CAST(COUNT(CASE WHEN a.isPresent = true THEN 1 ELSE null END) AS float) / COUNT(a) END DESC")
    List<Object[]> findClassesWithAttendanceRatesSorted();

    @Query(value = "SELECT EXTRACT(YEAR FROM a.attendance_date) AS yr, " +
            "EXTRACT(MONTH FROM a.attendance_date) AS mnth, " +
            "COUNT(a.id) AS total, " +
            "COUNT(CASE WHEN a.is_present = true THEN 1 ELSE null END) AS present " +
            "FROM attendance a WHERE a.attendance_date <= CURRENT_DATE " +
            "GROUP BY EXTRACT(YEAR FROM a.attendance_date), EXTRACT(MONTH FROM a.attendance_date) " +
            "ORDER BY EXTRACT(YEAR FROM a.attendance_date), EXTRACT(MONTH FROM a.attendance_date)",
            nativeQuery = true)
    List<Object[]> countAttendanceAndPresentByYearAndMonthUntilToday();

    List<Attendance> findByClassInfoId(Long classId);

    void deleteByClassInfoIdAndStudentId(Long classId, Long studentId);

    @Query("SELECT a FROM Attendance a WHERE a.classInfo = :classInfo AND a.attendanceDate = :date AND a.isPresent = :isPresent")
    List<Attendance> findByClassInfoAndAttendanceDateAndIsPresent(
            @Param("classInfo") Classes classInfo,
            @Param("date") LocalDate date,
            @Param("isPresent") boolean isPresent);

    /**
     * Tìm tất cả các bản ghi điểm danh theo lớp, ngày, trạng thái có mặt và trạng thái có phép
     */
    @Query("SELECT a FROM Attendance a WHERE a.classInfo = :classInfo AND a.attendanceDate = :date " +
            "AND a.isPresent = :isPresent AND a.isExcused = :isExcused")
    List<Attendance> findByClassInfoAndAttendanceDateAndIsPresentAndIsExcused(
            @Param("classInfo") Classes classInfo,
            @Param("date") LocalDate date,
            @Param("isPresent") boolean isPresent,
            @Param("isExcused") boolean isExcused);
}