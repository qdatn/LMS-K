package com.example.hcm25_cpl_ks_java_01_lms.role.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class RoleExistedException extends RuntimeException {
    private final HttpStatus status;

    public RoleExistedException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
