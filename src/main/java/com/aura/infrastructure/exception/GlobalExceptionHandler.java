package com.aura.infrastructure.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ─── Validation Errors (400) ────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (existing, replacement) -> existing
                ));

        log.warn("Validation failed for request to {}: {}", request.getRequestURI(), fieldErrors);

        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Failed",
                "One or more fields failed validation. See 'errors' for details.",
                request.getRequestURI(), fieldErrors);
    }

    // ─── Business Logic Errors (400) ─────────────────────────────────────────────

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRequest(
            InvalidRequestException ex, HttpServletRequest request) {

        log.warn("Invalid request to {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid Request",
                ex.getMessage(), request.getRequestURI(), null);
    }

    // ─── Not Found (404) ──────────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.warn("Resource not found at {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found",
                ex.getMessage(), request.getRequestURI(), null);
    }

    // ─── Terraform Execution Errors (500) ────────────────────────────────────────

    @ExceptionHandler(TerraformExecutionException.class)
    public ResponseEntity<Map<String, Object>> handleTerraformError(
            TerraformExecutionException ex, HttpServletRequest request) {

        log.error("Terraform execution error at {}: {}", request.getRequestURI(), ex.getMessage());
        Map<String, String> details = new HashMap<>();
        if (ex.getTerraformOutput() != null) {
            // Truncate overly long CLI output for API responses
            String truncated = ex.getTerraformOutput().length() > 2000
                    ? ex.getTerraformOutput().substring(0, 2000) + "... [truncated]"
                    : ex.getTerraformOutput();
            details.put("terraformOutput", truncated);
        }
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Terraform Execution Failed",
                ex.getMessage(), request.getRequestURI(), details.isEmpty() ? null : details);
    }

    // ─── Fallback (500) ───────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericError(
            Exception ex, HttpServletRequest request) {

        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please check server logs for details.",
                request.getRequestURI(), null);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status, String error, String message,
            String path, Object details) {

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("path", path);
        if (details != null) {
            body.put("errors", details);
        }
        return ResponseEntity.status(status).body(body);
    }
}
