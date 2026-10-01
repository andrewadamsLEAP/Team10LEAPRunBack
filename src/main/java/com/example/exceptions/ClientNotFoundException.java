package com.example.exceptions;

public class ClientNotFoundException extends RuntimeException {
    public ClientNotFoundException(String message) {
        super(message);
    }

    public ClientNotFoundException(Long clientId) {
        super("Client with ID " + clientId + " not found in or has no holdings");
    }
}