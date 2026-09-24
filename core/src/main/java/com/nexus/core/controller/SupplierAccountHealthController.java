package com.nexus.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.service.SupplierAccountHealthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/supplier/account-health")
@RequiredArgsConstructor
public class SupplierAccountHealthController {

    private final SupplierAccountHealthService healthService;

    @GetMapping("/{buyerOrgId}")
    @LogActivity("Get Account Health")
    public ResponseEntity<?> getAccountHealth(@PathVariable Long buyerOrgId) {
        return healthService.getAccountHealth(buyerOrgId);
    }

    @GetMapping("/all")
    @LogActivity("Get All Account Health")
    public ResponseEntity<?> getAllHealth() {
        return healthService.getAllAccountHealths();
    }

    @GetMapping("/summary")
    @LogActivity("Get Account Health Summary")
    public ResponseEntity<?> getSummary() {
        return healthService.getHealthSummary();
    }
}
