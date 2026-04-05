package com.wildbeyond.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralised exception handling for all REST controllers.
 *
 * Eliminates raw stack traces in API responses and returns structured JSON.
 *
 * Handled cases:
 *   ResourceNotFoundException       → 404 NOT FOUND
 *   AccessDeniedException           → 403 FORBIDDEN
 *   MethodArgumentNotValidException → 400 BAD REQUEST (field-level validation errors)
 *   RuntimeException (fallback)     → 400 BAD REQUEST (unexpected business logic errors)
 */
@RestControllerAdvice(basePackages = "com.wildbeyond.controller.rest")
public class GlobalExceptionHandler {

    /**
     * 404 — resource not found (Product, Seller, User etc.)
     *
     * Response example:
     * {
     *   "error": "Product not found with id: 5"
     * }
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    /**
     * 400 — @Valid /@Validated DTO validation failure.
     *
     * Response example:
     * {
     *   "name":  "Product name is required",
     *   "price": "Price must be a positive value"
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    /**
     * 400 — fallback for any other unexpected RuntimeException.
     *
     * Response example:
     * {
     *   "error": "An account with that email already exists."
     * }
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
