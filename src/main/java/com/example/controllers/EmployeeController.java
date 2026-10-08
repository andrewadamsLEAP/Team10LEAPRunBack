package com.example.controllers;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.DTOs.clients.ChangePasswordRequest;
import com.example.DTOs.clients.ClientAdminView;
import com.example.DTOs.employees.*;
import com.example.DTOs.clients.ClientReporterView;
import com.example.DTOs.clients.LoginRequest;
import com.example.DTOs.clients.LoginResponse;
import com.example.entities.Employee;
import com.example.services.ClientsService;
import com.example.services.EmployeeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {
 
    private final EmployeeService employeeService;
    private final ClientsService clientsService;

    public EmployeeController(EmployeeService employeeService, ClientsService clientsService) {
        this.employeeService = employeeService;
        this.clientsService = clientsService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(employeeService.login(request));
    }

    @PatchMapping("/password/{employeeId}")
    public ResponseEntity<Void> changePassword(@PathVariable Long employeeId, @RequestBody @Valid ChangePasswordRequest request) {
        employeeService.changePassword(employeeId, request);
        return ResponseEntity.noContent().build();
    }

    // Reporter endpoints - for reporting/analytics access
    @GetMapping("/reporter/{clientId}")
    public ResponseEntity<ClientReporterView> getClientAsReporter(@PathVariable Long clientId) {
        return ResponseEntity.ok(clientsService.getClientDataReporter(clientId));
    }

    @GetMapping("/reporter")
    public ResponseEntity<List<ClientReporterView>> getAllClientsAsReporter() {
        return ResponseEntity.ok(clientsService.getAllClientDataReporter());
    }
}


