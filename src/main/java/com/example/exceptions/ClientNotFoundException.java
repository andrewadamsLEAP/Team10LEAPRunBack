package com.example.exceptions;

public class ClientNotFoundException extends RuntimeException {
    public ClientNotFoundException(String message) {
        super(message);
    }

    public ClientNotFoundException(Long clientId, String resource) {
        super("Client with ID " + clientId + " not found in " + resource + " or has no holdings");
    }
}