package com.example.exceptions;

/**
 * Exception thrown when JWT validation or authentication fails.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
