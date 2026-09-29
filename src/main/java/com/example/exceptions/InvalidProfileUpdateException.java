package com.example.exceptions;

public class InvalidProfileUpdateException extends RuntimeException {
    public InvalidProfileUpdateException(String message) {
        super(message);
    }
}