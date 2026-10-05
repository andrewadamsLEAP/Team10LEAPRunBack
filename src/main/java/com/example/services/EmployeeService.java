package com.example.services;

import com.example.DTOs.clients.LoginResponse;

import java.util.List;

import com.example.DTOs.clients.ChangePasswordRequest;
import com.example.DTOs.clients.ClientProfileView;
import com.example.DTOs.clients.ClientReporterView;
import com.example.DTOs.clients.LoginRequest;
import com.example.DTOs.clients.LoginView;
import com.example.entities.*;
import com.example.exceptions.InvalidArgumentsException;
import com.example.exceptions.UpdateFailedException;
import com.example.exceptions.Validate;
import com.example.repositories.EmployeeRepository;

public class EmployeeService {
    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository)
    {
        this.employeeRepository = employeeRepository;
    }

    
    public LoginResponse login(LoginRequest request) {
        // TODO: Encode then compare passwords when we do the whole JwT node stuff
        LoginView loginEmployee = employeeRepository.findLoginEmployeeByUsername(request.username());

        if (loginEmployee == null || !loginEmployee.password().equals(request.password())) {
            throw new InvalidArgumentsException("Invalid Credentials", "Invalid username or password");
        }

        return new LoginResponse(loginEmployee.userId(), loginEmployee.username(), null, "Login successful");
    }

     public void changePassword(Long employeeId, ChangePasswordRequest request) {
        LoginView employee = employeeRepository.findLoginEmployeetById(employeeId);

        Validate.validateClient(employee);

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
        Employee employee = employeeRepository.findEmployeeById(employeeId);

        Validate.validateEmployee(employee);

        return new EmployeeProfileView(
                employee.getEmployeeId(),
                employee.getEmail(),
                employee.getUsername(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getRole());
    }
   

}
