package com.example.DTOs.employees;

import java.math.BigDecimal;
/**
 * DTO for representing client login information.
 * LoginView
 * @param employeeId
 * @param username
 * @param email
 * @param firstName
 * @param lastName
 * @param role
 */
public record EmployeeProfileView (
        Long employeeId,
        String email,
        String username,
        String firstName,
        String lastName,
        String role
){}
    

