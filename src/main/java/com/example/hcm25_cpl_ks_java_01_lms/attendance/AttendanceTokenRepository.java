package com.example.hcm25_cpl_ks_java_01_lms.attendance;

import com.example.hcm25_cpl_ks_java_01_lms.classes.Classes;
import com.example.hcm25_cpl_ks_java_01_lms.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceTokenRepository extends JpaRepository<AttendanceToken, String> {
    Optional<AttendanceToken> findByToken(String token);

    List<AttendanceToken> findByStudentAndClassInfo(User student, Classes classInfo);

    List<AttendanceToken> findByClassInfoAndIsUsed(Classes classInfo, Boolean isUsed);

    @Query("SELECT t FROM AttendanceToken t WHERE t.classInfo.id = :classId AND t.expiresAt > :now AND t.isUsed = false")
    List<AttendanceToken> findActiveTokensByClassId(@Param("classId") Long classId, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(t) FROM AttendanceToken t WHERE t.classInfo.id = :classId AND t.expiresAt > :now AND t.isUsed = false")
    int countActiveTokensByClassId(@Param("classId") Long classId, @Param("now") LocalDateTime now);
}