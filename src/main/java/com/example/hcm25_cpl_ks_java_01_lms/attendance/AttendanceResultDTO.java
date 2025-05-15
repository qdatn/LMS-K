package com.example.hcm25_cpl_ks_java_01_lms.attendance;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResultDTO {
    private String studentCode;
    private String studentName;
    private LocalDateTime time;
}
