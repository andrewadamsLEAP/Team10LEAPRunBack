package com.example.controllers;

import com.example.DTOs.clients.ClientProfileUpdateRequest;
import com.example.DTOs.clients.ClientProfileView;
import com.example.authUtils.ClientIdContextHolder;
import com.example.services.ClientsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientsController {
    private final ClientsService clientsService;

    public ClientsController(ClientsService clientsService) {
        this.clientsService = clientsService;
    }

    @PatchMapping("/profile")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Void> updateProfile(
            @RequestBody @Valid ClientProfileUpdateRequest request) {
        clientsService.updateProfile(ClientIdContextHolder.requireClientId(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ClientProfileView> viewProfile() {
        return ResponseEntity.ok(clientsService.getClientProfile(
                ClientIdContextHolder.requireClientId()));
    }
}
