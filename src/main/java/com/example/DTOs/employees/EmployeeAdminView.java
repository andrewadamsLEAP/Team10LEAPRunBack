package com.example.DTOs.employees;
/**
 * 
 * @param employeeId
 * @param username
 * @param email
 * @param firstName
 * @param lastName
 * @param role
 */
public record EmployeeAdminView (
        Long employeeId,
        String email,
        String username,
        String firstName,
        String lastName,
        String role
)
{}
