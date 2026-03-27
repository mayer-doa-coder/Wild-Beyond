package com.wildbeyond.exception;

/**
 * Thrown when an authenticated user attempts to access or modify
 * a resource they do not own.
 */
public class UnauthorizedAccessException extends RuntimeException {

    public UnauthorizedAccessException(String message) {
        super(message);
    }
}