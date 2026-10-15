package com.example.services;

import org.springframework.stereotype.Service;

import com.example.DTOs.clients.ChangePasswordRequest;
import com.example.DTOs.employees.EmployeeProfileView;
import com.example.DTOs.employees.EmployeeLoginView;
import com.example.authUtils.JwtUtil;
import com.example.DTOs.clients.LoginRequest;
import com.example.DTOs.clients.LoginView;
import com.example.entities.*;
import com.example.exceptions.InvalidArgumentsException;
import com.example.exceptions.UpdateFailedException;
import com.example.exceptions.Validate;
import com.example.repositories.EmployeeRepository;
import com.example.DTOs.clients.LoginResponse;

@Service 
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final JwtUtil jwtUtil;

    public EmployeeService(EmployeeRepository employeeRepository, JwtUtil jwtUtil)
    {
        this.employeeRepository = employeeRepository;
        this.jwtUtil = jwtUtil;
    }

    
    public LoginResponse login(LoginRequest request) {
        EmployeeLoginView loginEmployee = Validate.validateUser(
            employeeRepository.findLoginEmployeeByIdentifier(request.username()), "employee");
        
        if (loginEmployee == null || !loginEmployee.password().equals(request.password())) {
            throw new InvalidArgumentsException("Invalid Credentials", "Invalid username or password");
        }
       
        String role = normalizeRole(loginEmployee.role());
        String jwt = jwtUtil.generateToken(loginEmployee.username(), loginEmployee.userId(), null, role);
        return new LoginResponse(loginEmployee.userId(), loginEmployee.username(), jwt, "Login successful");
    }

    private String normalizeRole(String role) {
        return switch (role) {
            case "ADMIN", "EMPLOYEE_ADMIN" -> "EMPLOYEE_ADMIN";
            case "REPORTER", "EMPLOYEE_REPORTER" -> "EMPLOYEE_REPORTER";
            default -> throw new InvalidArgumentsException("Invalid Role", "Unsupported employee role");
        };
    }



    public void changePassword(Long employeeId, ChangePasswordRequest request) {
        LoginView employee = Validate.validateUser(employeeRepository.findLoginEmployeeById(employeeId), "employee");

        if (!employee.password().equals(request.currentPassword())) {
            throw new InvalidArgumentsException("Invalid Password Change", "Current password is incorrect");
        }

        if (request.currentPassword().equals(request.newPassword())) {
            throw new InvalidArgumentsException("Invalid Password Change", "New password must be different from current password");
        }

        int updatedRows = employeeRepository.updatePassword(employeeId, request.newPassword());

        if (updatedRows != 1) {
            throw new UpdateFailedException("Password update failed");
        }
    }



    public EmployeeProfileView getEmployeeProfile(Long employeeId) {
        Employee employee = Validate.validateUser(employeeRepository.findEmployeeById(employeeId), "employee");

        return new EmployeeProfileView(
                employee.getEmployeeId(),
                employee.getEmail(),
                employee.getUsername(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getRole());
    }
   

}
