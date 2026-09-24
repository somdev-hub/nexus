package com.nexus.core.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.payload.ProductVariantDto;
import com.nexus.core.service.ProductVariantService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/supplier/variants")
@RequiredArgsConstructor
public class ProductVariantController {

    private final ProductVariantService variantService;

    @PostMapping("/create")
    @LogActivity("Create Product Variant")
    public ResponseEntity<?> createVariant(@Valid @RequestBody ProductVariantDto dto) {
        return variantService.createVariant(dto);
    }

    @GetMapping("/{id}")
    @LogActivity("Get Product Variant")
    public ResponseEntity<?> getVariant(@PathVariable Long id) {
        return variantService.getVariant(id);
    }

    @GetMapping("/all")
    @LogActivity("Get All Product Variants")
    public ResponseEntity<?> getAllVariants(
            @RequestParam(required = false) Long catalogId,
            @RequestParam(required = false) String variantType,
            @RequestParam(required = false) Long bomMaterialId,
            @PageableDefault(size = 20) Pageable pageable) {
        return variantService.getAllVariants(catalogId, variantType, bomMaterialId, pageable);
    }

    @PutMapping("/{id}/update")
    @LogActivity("Update Product Variant")
    public ResponseEntity<?> updateVariant(@PathVariable Long id, @RequestBody ProductVariantDto dto) {
        return variantService.updateVariant(id, dto);
    }

    @DeleteMapping("/{id}")
    @LogActivity("Delete Product Variant")
    public ResponseEntity<?> deleteVariant(@PathVariable Long id) {
        return variantService.deleteVariant(id);
    }
}
