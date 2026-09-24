package com.nexus.core.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.payload.SupplierPriceTierDto;
import com.nexus.core.service.SupplierPriceTierService;

import lombok.RequiredArgsConstructor;

import java.sql.Date;

@RestController
@RequestMapping("/core/supplier/price-tiers")
@RequiredArgsConstructor
public class SupplierPriceTierController {

    private final SupplierPriceTierService priceTierService;

    @PostMapping("/create")
    @LogActivity("Create Supplier Price Tier")
    public ResponseEntity<?> createPriceTier(@Valid @RequestBody SupplierPriceTierDto dto) {
        return priceTierService.createPriceTier(dto);
    }

    @GetMapping("/{id}")
    @LogActivity("Get Supplier Price Tier")
    public ResponseEntity<?> getPriceTier(@PathVariable Long id) {
        return priceTierService.getPriceTier(id);
    }

    @GetMapping("/all")
    @LogActivity("Get All Supplier Price Tiers")
    public ResponseEntity<?> getAllPriceTiers(
            @RequestParam(required = false) Long catalogId,
            @RequestParam(required = false) String customerSegment,
            @RequestParam(required = false) Long contractId,
            @RequestParam(required = false) Date validFrom,
            @RequestParam(required = false) Date validTo,
            @PageableDefault(size = 20) Pageable pageable) {
        return priceTierService.getAllPriceTiers(catalogId, customerSegment, contractId, validFrom, validTo, pageable);
    }

    @PutMapping("/{id}/update")
    @LogActivity("Update Supplier Price Tier")
    public ResponseEntity<?> updatePriceTier(@PathVariable Long id, @RequestBody SupplierPriceTierDto dto) {
        return priceTierService.updatePriceTier(id, dto);
    }

    @DeleteMapping("/{id}")
    @LogActivity("Delete Supplier Price Tier")
    public ResponseEntity<?> deletePriceTier(@PathVariable Long id) {
        return priceTierService.deletePriceTier(id);
    }

    @GetMapping("/price-for-quantity")
    @LogActivity("Get Price For Quantity")
    public ResponseEntity<?> getPriceForQuantity(
            @RequestParam Long catalogId,
            @RequestParam Double quantity,
            @RequestParam(required = false) String customerSegment) {
        return priceTierService.getPriceForQuantity(catalogId, quantity, customerSegment);
    }
}
