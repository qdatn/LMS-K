package com.example.hcm25_cpl_ks_java_01_lms.auth.exception;

import com.example.hcm25_cpl_ks_java_01_lms.auth.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.example.hcm25_cpl_ks_java_01_lms.auth")
public class AuthExceptionHandler {
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthException ex) {
        ErrorResponse error = new ErrorResponse("AUTH_ERROR", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserLockedException.class)
    public ResponseEntity<ErrorResponse> handleUserLockedException(UserLockedException ex) {
        ErrorResponse error = new ErrorResponse("USER_LOCKED", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(OtpInvalidException.class)
    public ResponseEntity<ErrorResponse> handleOtpInvalidException(OtpInvalidException ex) {
        ErrorResponse error = new ErrorResponse("OTP_INVALID", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
