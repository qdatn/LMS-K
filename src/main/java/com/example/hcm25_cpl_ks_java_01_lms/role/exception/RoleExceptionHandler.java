package com.example.hcm25_cpl_ks_java_01_lms.role.exception;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.hcm25_cpl_ks_java_01_lms.common.exceptionhandler.ErrorResponse;

@RestControllerAdvice
public class RoleExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getStatus(),
                ex.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

    @ExceptionHandler(RoleNameTooLongException.class)
    public ResponseEntity<?> handleRoleNameTooLongException(RoleNameTooLongException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<Object> handleRoleNotFoundException(RoleNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getStatus(),
                ex.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

    @ExceptionHandler(RoleExistedException.class)
    public ResponseEntity<Object> handleRoleExistedException(RoleExistedException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getStatus(),
                ex.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }
}
