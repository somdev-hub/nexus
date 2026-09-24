package com.nexus.core.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.SupplierQualityCertificateDto;

public interface SupplierQualityCertificateService {

    ResponseEntity<?> createCertificate(SupplierQualityCertificateDto dto);

    ResponseEntity<?> getCertificate(Long id);

    ResponseEntity<?> getAllCertificates(Long purchaseOrderId, Long shipmentId, Long catalogId, String certificateType, Pageable pageable);

    ResponseEntity<?> updateCertificate(Long id, SupplierQualityCertificateDto dto);

    ResponseEntity<?> deleteCertificate(Long id);
}
