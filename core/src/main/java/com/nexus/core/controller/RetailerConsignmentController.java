package com.nexus.core.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.service.interfaces.ConsignmentStockService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/retailer/consignment")
@RequiredArgsConstructor
public class RetailerConsignmentController {

    private final ConsignmentStockService consignmentService;

    @GetMapping("/all")
    @LogActivity("Get Retailer Consignments")
    public ResponseEntity<?> getAll(@RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long materialId,
            @PageableDefault(size = 20) Pageable pageable) {
        return consignmentService.getRetailerConsignments(warehouseId, materialId, pageable);
    }

    @PostMapping("/{id}/consume")
    @LogActivity("Consume Consignment")
    public ResponseEntity<?> consume(@PathVariable Long id, @RequestParam Double quantity) {
        return consignmentService.consumeConsignmentByRetailer(id, quantity);
    }

    @PostMapping("/{id}/return")
    @LogActivity("Return Consignment")
    public ResponseEntity<?> returnConsignment(@PathVariable Long id, @RequestParam Double quantity) {
        return consignmentService.returnConsignmentByRetailer(id, quantity);
    }

    @PostMapping("/{id}/receive")
    @LogActivity("Receive Consignment")
    public ResponseEntity<?> receiveConsignment(@PathVariable Long id, @RequestParam Double quantity) {
        return consignmentService.receiveConsignmentByRetailer(id, quantity);
    }
}