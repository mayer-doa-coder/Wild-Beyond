package com.wildbeyond.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource does not exist in the database.
 *
 * Caught by GlobalExceptionHandler → returns 404 NOT FOUND.
 *
 * Examples:
 *   - Product with id 99 not found
 *   - Seller with id 7 not found
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
