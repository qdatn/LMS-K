package com.example.hcm25_cpl_ks_java_01_lms.role.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import lombok.Getter;

@ResponseStatus(HttpStatus.NOT_FOUND)
@Getter
public class RoleNotFoundException extends RuntimeException {
    private final HttpStatus status;

    public RoleNotFoundException(String message) {
        super(message);
        this.status = HttpStatus.NOT_FOUND;
    }
}
