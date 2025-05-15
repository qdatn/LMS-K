package com.example.hcm25_cpl_ks_java_01_lms.attendance;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceDTO {
    private Long id;
    private String name;
    private String classCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private int studentCount;
}