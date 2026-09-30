package com.example.DTOs.clients;

import jakarta.validation.constraints.Size;

public class ClientProfileUpdateRequest {
    @Size(min = 3, max = 100)
    private String username;

    @Size(min = 1, max = 100)
    private String firstName;

    @Size(min = 1, max = 100)
    private String lastName;

    public ClientProfileUpdateRequest() {
    }

    public boolean hasUpdates() {
        return hasText(username) || hasText(firstName) || hasText(lastName);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}