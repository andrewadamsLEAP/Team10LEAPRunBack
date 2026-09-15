package com.example.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.services.PriceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prices")
//@CrossOrigin(origins = "http://localhost:${server.port}")
public class PriceController {
	private final PriceService priceService;

    public PriceController(PriceService priceService) {this.priceService = priceService;}

    @GetMapping("/test")
    public String test() {
        try {
            return "Test endpoint is working!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error executing sample query: " + e.getMessage();
        }
    }

    @PostMapping("/fetch")
    public ResponseEntity<Void> addNewPrices() {
        try {
            priceService.pullData();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

}
