package com.fooddrinks.exception;

import com.fooddrinks.common.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<?>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<?>> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(409, ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<?>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, ex.getMessage()));
    }

    // Handles DB unique constraint violations — parses MySQL root cause to return a
    // specific message
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String msg = ex.getMostSpecificCause().getMessage();
        String friendlyMessage = resolveIntegrityMessage(msg);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(409, friendlyMessage));
    }

    private String resolveIntegrityMessage(String causeMessage) {
        if (causeMessage == null)
            return "Duplicate entry — the resource already exists";
        String lower = causeMessage.toLowerCase();
        if (lower.contains("users") && lower.contains("email")) {
            return "Email already in use — please use a different email";
        }
        if (lower.contains("cart_items") || (lower.contains("cart") && lower.contains("product"))) {
            return "This product is already in your cart — use the update endpoint to change quantity";
        }
        if (lower.contains("categories") && lower.contains("name")) {
            return "Category name already exists — please use a different name";
        }
        if (lower.contains("ratings") || (lower.contains("user") && lower.contains("product"))) {
            return "You have already rated this product";
        }
        return "Duplicate entry — the resource already exists";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        // Use getFieldErrors() to avoid ClassCastException from bean-level ObjectErrors
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        // Collect bean-level (class-level) constraint violations under "_global"
        for (ObjectError oe : ex.getBindingResult().getGlobalErrors()) {
            errors.put("_global", oe.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, "Validation failed", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Internal server error"));
    }
}
