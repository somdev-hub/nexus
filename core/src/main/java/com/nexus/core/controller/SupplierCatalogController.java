package com.nexus.core.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.payload.SupplierCatalogDto;
import com.nexus.core.service.SupplierCatalogService;

import lombok.RequiredArgsConstructor;

import java.util.Map;

@RestController
@RequestMapping("/core/supplier/catalog")
@RequiredArgsConstructor
public class SupplierCatalogController {

    private final SupplierCatalogService catalogService;

    @PostMapping("/create")
    @LogActivity("Create Supplier Catalog")
    public ResponseEntity<?> createCatalog(@Valid @RequestBody SupplierCatalogDto dto) {
        return catalogService.createCatalog(dto);
    }

    @GetMapping("/{id}")
    @LogActivity("Get Supplier Catalog")
    public ResponseEntity<?> getCatalog(@PathVariable Long id) {
        return catalogService.getCatalog(id);
    }

    @GetMapping("/all")
    @LogActivity("Get All Supplier Catalogs")
    public ResponseEntity<?> getAllCatalogs(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String family,
            @RequestParam(required = false) String accessLevel,
            @RequestParam(required = false) Boolean isPublished,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return catalogService.getAllCatalogs(status, category, family, accessLevel, isPublished, search, pageable);
    }

    @PutMapping("/{id}/update")
    @LogActivity("Update Supplier Catalog")
    public ResponseEntity<?> updateCatalog(@PathVariable Long id, @RequestBody SupplierCatalogDto dto) {
        return catalogService.updateCatalog(id, dto);
    }

    @PutMapping("/{id}/status")
    @LogActivity("Transition Supplier Catalog Status")
    public ResponseEntity<?> transitionStatus(@PathVariable Long id,
            @RequestParam String newStatus,
            @RequestBody(required = false) Map<String, Object> params) {
        return catalogService.transitionStatus(id, newStatus, params);
    }

    @DeleteMapping("/{id}")
    @LogActivity("Delete Supplier Catalog")
    public ResponseEntity<?> deleteCatalog(@PathVariable Long id) {
        return catalogService.deleteCatalog(id);
    }

    @GetMapping("/summary")
    @LogActivity("Get Supplier Catalog Summary")
    public ResponseEntity<?> getSummary() {
        return catalogService.getSummary();
    }
}
