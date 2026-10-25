package com.nexus.core.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.payload.ProductionCapacityDto;
import com.nexus.core.service.interfaces.ProductionCapacityService;

import lombok.RequiredArgsConstructor;

import java.sql.Date;

@RestController
@RequestMapping("/core/supplier/capacity")
@RequiredArgsConstructor
public class ProductionCapacityController {

    private final ProductionCapacityService capacityService;

    @PostMapping("/create")
    @LogActivity("Create Production Capacity")
    public ResponseEntity<?> createCapacity(@Valid @RequestBody ProductionCapacityDto dto) {
        return capacityService.createCapacity(dto);
    }

    @GetMapping("/{id}")
    @LogActivity("Get Production Capacity")
    public ResponseEntity<?> getCapacity(@PathVariable Long id) {
        return capacityService.getCapacity(id);
    }

    @GetMapping("/all")
    @LogActivity("Get All Production Capacities")
    public ResponseEntity<?> getAllCapacities(
            @RequestParam(required = false) String productLine,
            @RequestParam(required = false) String shift,
            @RequestParam(required = false) Date periodStart,
            @RequestParam(required = false) Date periodEnd,
            @PageableDefault(size = 20) Pageable pageable) {
        return capacityService.getAllCapacities(productLine, shift, periodStart, periodEnd, pageable);
    }

    @PutMapping("/{id}/update")
    @LogActivity("Update Production Capacity")
    public ResponseEntity<?> updateCapacity(@PathVariable Long id, @RequestBody ProductionCapacityDto dto) {
        return capacityService.updateCapacity(id, dto);
    }

    @DeleteMapping("/{id}")
    @LogActivity("Delete Production Capacity")
    public ResponseEntity<?> deleteCapacity(@PathVariable Long id) {
        return capacityService.deleteCapacity(id);
    }

    @GetMapping("/summary")
    @LogActivity("Get Capacity Summary")
    public ResponseEntity<?> getSummary() {
        return capacityService.getCapacitySummary();
    }

    /**
     * Read-only check used by the quotation dialog: remaining capacity for a
     * catalog item (+ optional variant) over the requested window, compared
     * against the requested quantity.
     */
    @GetMapping("/availability")
    @LogActivity("Check Capacity Availability")
    public ResponseEntity<?> checkAvailability(@RequestParam Long catalogId,
            @RequestParam(required = false) Long variantId,
            @RequestParam(required = false) Double quantity,
            @RequestParam(required = false) Date periodStart,
            @RequestParam(required = false) Date periodEnd) {
        return capacityService.checkAvailability(catalogId, variantId, quantity, periodStart, periodEnd);
    }
}
