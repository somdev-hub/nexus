package com.nexus.core.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.payload.ConsignmentStockDto;
import com.nexus.core.service.ConsignmentStockService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/supplier/consignment")
@RequiredArgsConstructor
public class ConsignmentStockController {

    private final ConsignmentStockService consignmentService;

    @PostMapping("/create")
    @LogActivity("Create Consignment Stock")
    public ResponseEntity<?> createConsignment(@Valid @RequestBody ConsignmentStockDto dto) {
        return consignmentService.createConsignment(dto);
    }

    @GetMapping("/{id}")
    @LogActivity("Get Consignment Stock")
    public ResponseEntity<?> getConsignment(@PathVariable Long id) {
        return consignmentService.getConsignment(id);
    }

    @GetMapping("/all")
    @LogActivity("Get All Consignment Stocks")
    public ResponseEntity<?> getAllConsignments(
            @RequestParam(required = false) Long retailerOrgId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long materialId,
            @PageableDefault(size = 20) Pageable pageable) {
        return consignmentService.getAllConsignments(retailerOrgId, warehouseId, materialId, pageable);
    }

    @PutMapping("/{id}/update")
    @LogActivity("Update Consignment Stock")
    public ResponseEntity<?> updateConsignment(@PathVariable Long id, @RequestBody ConsignmentStockDto dto) {
        return consignmentService.updateConsignment(id, dto);
    }

    @PostMapping("/{id}/adjust")
    @LogActivity("Adjust Consignment Quantity")
    public ResponseEntity<?> adjustQuantity(@PathVariable Long id,
            @RequestParam Double quantity,
            @RequestParam String reason) {
        return consignmentService.adjustQuantity(id, quantity, reason);
    }

    @DeleteMapping("/{id}")
    @LogActivity("Delete Consignment Stock")
    public ResponseEntity<?> deleteConsignment(@PathVariable Long id) {
        return consignmentService.deleteConsignment(id);
    }

    @GetMapping("/summary")
    @LogActivity("Get Consignment Summary")
    public ResponseEntity<?> getSummary() {
        return consignmentService.getSummary();
    }
}
