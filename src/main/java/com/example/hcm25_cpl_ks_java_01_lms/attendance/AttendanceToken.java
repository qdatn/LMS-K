package com.example.hcm25_cpl_ks_java_01_lms.attendance;

import com.example.hcm25_cpl_ks_java_01_lms.classes.Classes;
import com.example.hcm25_cpl_ks_java_01_lms.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Builder
@Table(name = "attendance_token")  // Giữ nguyên tên bảng ban đầu
public class AttendanceToken {

    @Id
    private String token;  // Giữ nguyên cấu trúc key ban đầu

    @ManyToOne
    @JoinColumn(name = "class_id", nullable = false)
    private Classes classInfo;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean isUsed = false;

    @Column
    private LocalDateTime usedAt;

    @Column(nullable = true)  // Đặt nullable để tương thích với dữ liệu đã có
    private LocalDateTime createdAt;  // Không đặt giá trị mặc định ở đây
}