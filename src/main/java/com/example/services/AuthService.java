package com.example.services;

import com.example.DTOs.clients.AuthLoginResponse;
import com.example.DTOs.clients.LoginRequest;
import com.example.DTOs.clients.LoginView;
import com.example.DTOs.employees.EmployeeLoginView;
import com.example.authUtils.JwtUtil;
import com.example.exceptions.InvalidArgumentsException;
import com.example.repositories.ClientsRepository;
import com.example.repositories.EmployeeRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

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
        String identifier = request.username().trim();
        LoginView client = clientsRepository.findLoginClientByIdentifier(identifier);
        EmployeeLoginView employee = employeeRepository.findLoginEmployeeByIdentifier(identifier);

        if (client != null && employee != null) {
            throw new InvalidArgumentsException("Invalid Credentials", "Username or email is ambiguous");
        }

        if (client != null && passwordMatches(request.password(), client.password())) {
            String token = jwtUtil.generateToken(client.userId(), client.username(), "client", "CLIENT");
            return new AuthLoginResponse(client.userId(), client.username(), "CLIENT", token, "Login successful");
        }

        if (employee != null && passwordMatches(request.password(), employee.password())) {
            String role = normalizeEmployeeRole(employee.role());
            String token = jwtUtil.generateToken(employee.userId(), employee.username(), "employee", role);
            return new AuthLoginResponse(employee.userId(), employee.username(), role, token, "Login successful");
        }

        throw new InvalidArgumentsException("Invalid Credentials", "Invalid username or password");
    }

    private String normalizeEmployeeRole(String role) {
        return switch (role.toUpperCase()) {
            case "ADMIN", "EMPLOYEE_ADMIN" -> "EMPLOYEE_ADMIN";
            case "REPORTER", "EMPLOYEE_REPORTER" -> "EMPLOYEE_REPORTER";
            default -> throw new InvalidArgumentsException("Invalid Role", "Unsupported employee role");
        };
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }

        if (storedPassword.startsWith("$2a$")
                || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$")) {
            return PASSWORD_ENCODER.matches(rawPassword, storedPassword);
        }

        // Keep legacy plaintext development rows working during migration.
        return storedPassword.equals(rawPassword);
    }
}