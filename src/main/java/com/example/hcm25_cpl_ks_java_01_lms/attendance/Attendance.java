package com.example.hcm25_cpl_ks_java_01_lms.attendance;

import com.example.hcm25_cpl_ks_java_01_lms.classes.Classes;
import com.example.hcm25_cpl_ks_java_01_lms.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Builder
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "class_id", nullable = true)
    private Classes classInfo;


    @ManyToOne
    @JoinColumn(name = "student_id", nullable = true)
    private User student;

    @Column(nullable = false)
    private LocalDate attendanceDate;

    @Column
    private LocalDateTime checkedInTime;

    @Column
    private Boolean isPresent = false;

    @Column
    private Boolean isExcused = false;

    @Column(length = 500)
    private String notes;
}
