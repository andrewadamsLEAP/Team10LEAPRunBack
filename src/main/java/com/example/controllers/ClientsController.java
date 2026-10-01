package com.example.controllers;

import com.example.DTOs.clients.ChangePasswordRequest;
import com.example.DTOs.clients.ClientAdminView;
import com.example.DTOs.clients.ClientProfileUpdateRequest;
import com.example.DTOs.clients.ClientProfileView;
import com.example.DTOs.clients.ClientReporterView;
import com.example.DTOs.clients.LoginRequest;
import com.example.DTOs.clients.LoginResponse;
import com.example.entities.Client;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.services.ClientsService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientsController {
    private final ClientsService clientsService;

    public ClientsController(ClientsService clientsService) {
        this.clientsService = clientsService;
    }

    @PostMapping("/sign-up")
    public ResponseEntity<LoginResponse> signup(
            @RequestBody @Valid Client request) {
        return ResponseEntity.ok(clientsService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(clientsService.login(request));
    }

    @PatchMapping("/password/{clientId}")
    public ResponseEntity<Void> changePassword( 
            @PathVariable Long clientId,
            @RequestBody @Valid ChangePasswordRequest request) {
        clientsService.changePassword(clientId, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/profile/{clientId}")
    public ResponseEntity<Void> updateProfile( 
            @PathVariable Long clientId,
            @RequestBody @Valid ClientProfileUpdateRequest request) {
        clientsService.updateProfile(clientId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/profile/{clientId}")
    public ResponseEntity<ClientProfileView> viewProfile(@PathVariable Long clientId) {
        return ResponseEntity.ok(clientsService.getClientProfile(clientId));
    }

    // Admin endpoints - for administrative access
    @GetMapping("/admin/{clientId}")
    public ResponseEntity<ClientAdminView> getClientAsAdmin(@PathVariable Long clientId) {
        return ResponseEntity.ok(clientsService.getClientDataAdmin(clientId));
    }

    @GetMapping("/admin")
    public ResponseEntity<List<ClientAdminView>> getAllClientsAsAdmin() {
        return ResponseEntity.ok(clientsService.getAllClientDataAdmin());
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
