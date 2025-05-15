package com.example.hcm25_cpl_ks_java_01_lms.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpDTO {
    private String otp;
    private String action;
}
