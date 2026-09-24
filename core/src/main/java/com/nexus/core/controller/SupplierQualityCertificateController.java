package com.nexus.core.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.payload.SupplierQualityCertificateDto;
import com.nexus.core.service.SupplierQualityCertificateService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/supplier/quality-certificates")
@RequiredArgsConstructor
public class SupplierQualityCertificateController {

    private final SupplierQualityCertificateService certService;

    @PostMapping("/create")
    @LogActivity("Create Quality Certificate")
    public ResponseEntity<?> createCertificate(@Valid @RequestBody SupplierQualityCertificateDto dto) {
        return certService.createCertificate(dto);
    }

    @GetMapping("/{id}")
    @LogActivity("Get Quality Certificate")
    public ResponseEntity<?> getCertificate(@PathVariable Long id) {
        return certService.getCertificate(id);
    }

    @GetMapping("/all")
    @LogActivity("Get All Quality Certificates")
    public ResponseEntity<?> getAllCertificates(
            @RequestParam(required = false) Long purchaseOrderId,
            @RequestParam(required = false) Long shipmentId,
            @RequestParam(required = false) Long catalogId,
            @RequestParam(required = false) String certificateType,
            @PageableDefault(size = 20) Pageable pageable) {
        return certService.getAllCertificates(purchaseOrderId, shipmentId, catalogId, certificateType, pageable);
    }

    @PutMapping("/{id}/update")
    @LogActivity("Update Quality Certificate")
    public ResponseEntity<?> updateCertificate(@PathVariable Long id, @RequestBody SupplierQualityCertificateDto dto) {
        return certService.updateCertificate(id, dto);
    }

    @DeleteMapping("/{id}")
    @LogActivity("Delete Quality Certificate")
    public ResponseEntity<?> deleteCertificate(@PathVariable Long id) {
        return certService.deleteCertificate(id);
    }
}
