package com.example.clients.DTO;

public class AuthResponse {
    private boolean authenticated;
    private Long clientId;
    private String username;
    private String token;
    private String message;

    public AuthResponse() {
    }

    public AuthResponse(boolean authenticated, Long clientId, String username, String token, String message) {
        this.authenticated = authenticated;
        this.clientId = clientId;
        this.username = username;
        this.token = token;
        this.message = message;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}