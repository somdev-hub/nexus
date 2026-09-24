package com.nexus.core.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.payload.SupplierDigitalAssetDto;
import com.nexus.core.service.SupplierDigitalAssetService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/supplier/digital-assets")
@RequiredArgsConstructor
public class SupplierDigitalAssetController {

    private final SupplierDigitalAssetService assetService;

    @PostMapping("/create")
    @LogActivity("Create Supplier Digital Asset")
    public ResponseEntity<?> createAsset(@Valid @RequestBody SupplierDigitalAssetDto dto) {
        return assetService.createAsset(dto);
    }

    @GetMapping("/{id}")
    @LogActivity("Get Supplier Digital Asset")
    public ResponseEntity<?> getAsset(@PathVariable Long id) {
        return assetService.getAsset(id);
    }

    @GetMapping("/all")
    @LogActivity("Get All Supplier Digital Assets")
    public ResponseEntity<?> getAllAssets(
            @RequestParam(required = false) Long catalogId,
            @RequestParam(required = false) String assetType,
            @PageableDefault(size = 20) Pageable pageable) {
        return assetService.getAllAssets(catalogId, assetType, pageable);
    }

    @PutMapping("/{id}/update")
    @LogActivity("Update Supplier Digital Asset")
    public ResponseEntity<?> updateAsset(@PathVariable Long id, @RequestBody SupplierDigitalAssetDto dto) {
        return assetService.updateAsset(id, dto);
    }

    @DeleteMapping("/{id}")
    @LogActivity("Delete Supplier Digital Asset")
    public ResponseEntity<?> deleteAsset(@PathVariable Long id) {
        return assetService.deleteAsset(id);
    }
}
