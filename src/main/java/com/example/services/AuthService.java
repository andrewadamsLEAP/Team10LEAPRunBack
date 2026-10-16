package com.example.services;

import com.example.DTOs.clients.AuthLoginResponse;
import com.example.DTOs.clients.LoginRequest;
import com.example.DTOs.clients.LoginView;
import com.example.DTOs.employees.EmployeeLoginView;
import com.example.authUtils.JwtUtil;
import com.example.exceptions.InvalidArgumentsException;
import com.example.repositories.ClientsRepository;
import com.example.repositories.EmployeeRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final ClientsRepository clientsRepository;
    private final EmployeeRepository employeeRepository;
    private final JwtUtil jwtUtil;

    public AuthService(
            ClientsRepository clientsRepository,
            EmployeeRepository employeeRepository,
            JwtUtil jwtUtil) {
        this.clientsRepository = clientsRepository;
        this.employeeRepository = employeeRepository;
        this.jwtUtil = jwtUtil;
    }

    public AuthLoginResponse login(LoginRequest request) {
        LoginView client = clientsRepository.findLoginClientByIdentifier(request.username());
        EmployeeLoginView employee = employeeRepository.findLoginEmployeeByIdentifier(request.username());

        if (client != null && employee != null) {
            throw new InvalidArgumentsException("Invalid Credentials", "Username or email is ambiguous");
        }

        if (client != null && client.password().equals(request.password())) {
            String token = jwtUtil.generateToken();
            return new AuthLoginResponse(client.userId(), client.username(), "CLIENT", token, "Login successful");
        }

        if (employee != null && employee.password().equals(request.password())) {
            String role = normalizeEmployeeRole(employee.role());
            String token = jwtUtil.generateToken();
            return new AuthLoginResponse(employee.userId(), employee.username(), role, token, "Login successful");
        }

        throw new InvalidArgumentsException("Invalid Credentials", "Invalid username or password");
    }

    private String normalizeEmployeeRole(String role) {
        return switch (role) {
            case "ADMIN", "EMPLOYEE_ADMIN" -> "EMPLOYEE_ADMIN";
            case "REPORTER", "EMPLOYEE_REPORTER" -> "EMPLOYEE_REPORTER";
            default -> throw new InvalidArgumentsException("Invalid Role", "Unsupported employee role");
        };
    }
}