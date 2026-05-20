package com.bank.core.controller;

import com.bank.common.dto.UniversalResponse;
import com.bank.common.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<UniversalResponse<Object>> handleBaseException(BaseException ex) {
        log.error("Handling BaseException: {}", ex.getMessage());
        return ResponseEntity
                .status(ex.getHttpCode())
                .body(new UniversalResponse<>(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<UniversalResponse<Object>> handleValidationException(
            MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.error("Validation error: {}", message);
        return ResponseEntity.status(400).body(new UniversalResponse<>(4001, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<UniversalResponse<Object>> handleGeneralException(Exception ex) {
        log.error("Handling general exception", ex);
        return ResponseEntity.status(500)
                .body(new UniversalResponse<>(5000, "Internal Server Error: " + ex.getMessage()));
    }
}
