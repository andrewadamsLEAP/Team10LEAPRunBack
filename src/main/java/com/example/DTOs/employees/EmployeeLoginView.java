package com.example.DTOs.employees;

public record EmployeeLoginView(
        Long userId,
        String username,
        String password,
        String role
) {}