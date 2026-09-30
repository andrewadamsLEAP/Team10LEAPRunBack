package com.example.exceptions;

public class InvalidArgumentsException extends RuntimeException {
    public InvalidArgumentsException(String exceptionType, String message) {
        super(exceptionType + ": " + message);
    }
}