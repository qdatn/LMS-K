package com.example.hcm25_cpl_ks_java_01_lms.role.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class RoleNameTooLongException extends RuntimeException {
    private final HttpStatus status;

    public RoleNameTooLongException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
