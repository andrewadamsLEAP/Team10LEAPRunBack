package com.example.exceptions;

public class InvalidUsernameChangeException extends RuntimeException {
    public InvalidUsernameChangeException(String message) {
        super(message);
    }
}