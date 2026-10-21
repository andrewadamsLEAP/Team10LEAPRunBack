package com.example.exceptions;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(Long clientId) {
        super("User with ID " + clientId + " not foundor has no holdings");
    }
}