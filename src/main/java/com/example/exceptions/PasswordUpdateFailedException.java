package com.example.exceptions;

public class PasswordUpdateFailedException extends RuntimeException {
    public PasswordUpdateFailedException(String message) {
        super(message);
    }
}