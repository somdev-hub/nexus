package com.nexus.core.service.impl;

import com.nexus.core.entities.Account;
import com.nexus.core.entities.PurchaseOrder;
import com.nexus.core.entities.Shipment;
import com.nexus.core.entities.SupplierCatalog;
import com.nexus.core.entities.SupplierQualityCertificate;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.SupplierQualityCertificateDto;
import com.nexus.core.repository.AccountRepository;
import com.nexus.core.repository.PurchaseOrderRepo;
import com.nexus.core.repository.ShipmentRepo;
import com.nexus.core.repository.SupplierCatalogRepo;
import com.nexus.core.repository.SupplierQualityCertificateRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.SupplierQualityCertificateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierQualityCertificateServiceImpl implements SupplierQualityCertificateService {

    private final SupplierQualityCertificateRepo certRepo;
    private final SupplierCatalogRepo catalogRepo;
    private final PurchaseOrderRepo poRepo;
    private final ShipmentRepo shipmentRepo;
    private final AccountRepository accountRepo;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public ResponseEntity<?> createCertificate(SupplierQualityCertificateDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        Account org = accountRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", orgId));
        SupplierQualityCertificate cert = modelMapper.map(dto, SupplierQualityCertificate.class);
        cert.setCertificateId(null);
        cert.setSupplierOrg(org);
        if (dto.getPurchaseOrderId() != null) {
            PurchaseOrder po = poRepo.findById(dto.getPurchaseOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "purchaseOrderId", dto.getPurchaseOrderId()));
            cert.setPurchaseOrder(po);
        }
        if (dto.getShipmentId() != null) {
            Shipment sh = shipmentRepo.findById(dto.getShipmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Shipment", "shipmentId", dto.getShipmentId()));
            cert.setShipment(sh);
        }
        if (dto.getCatalogId() != null) {
            SupplierCatalog catalog = catalogRepo.findByCatalogIdAndSupplierOrgAccountId(dto.getCatalogId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("SupplierCatalog", "catalogId", dto.getCatalogId()));
            cert.setCatalog(catalog);
        }
        SupplierQualityCertificate saved = certRepo.save(cert);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCertificate(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierQualityCertificate cert = certRepo.findByCertificateIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierQualityCertificate", "certificateId", id));
        return ResponseEntity.ok(mapToDto(cert));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllCertificates(Long purchaseOrderId, Long shipmentId, Long catalogId, String certificateType, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierQualityCertificate.CertificateType ct = null;
        if (certificateType != null && !certificateType.isBlank()) {
            try { ct = SupplierQualityCertificate.CertificateType.valueOf(certificateType.toUpperCase()); }
            catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(java.util.Map.of("error", "Invalid certificateType: " + certificateType)); }
        }
        Page<SupplierQualityCertificate> page = certRepo.findByOrgWithFilters(orgId, purchaseOrderId, shipmentId, catalogId, ct, pageable);
        Page<SupplierQualityCertificateDto> dtoPage = page.map(this::mapToDto);
        return ResponseEntity.ok(dtoPage);
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateCertificate(Long id, SupplierQualityCertificateDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierQualityCertificate cert = certRepo.findByCertificateIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierQualityCertificate", "certificateId", id));
        if (dto.getCertificateType() != null) cert.setCertificateType(dto.getCertificateType());
        if (dto.getCertificateNumber() != null) cert.setCertificateNumber(dto.getCertificateNumber());
        if (dto.getDmsDocumentId() != null) cert.setDmsDocumentId(dto.getDmsDocumentId());
        if (dto.getDmsDocumentUrl() != null) cert.setDmsDocumentUrl(dto.getDmsDocumentUrl());
        if (dto.getIssuedDate() != null) cert.setIssuedDate(dto.getIssuedDate());
        if (dto.getExpiryDate() != null) cert.setExpiryDate(dto.getExpiryDate());
        if (dto.getNotes() != null) cert.setNotes(dto.getNotes());
        SupplierQualityCertificate saved = certRepo.save(cert);
        return ResponseEntity.ok(mapToDto(saved));
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteCertificate(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierQualityCertificate cert = certRepo.findByCertificateIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierQualityCertificate", "certificateId", id));
        certRepo.delete(cert);
        return ResponseEntity.noContent().build();
    }

    private SupplierQualityCertificateDto mapToDto(SupplierQualityCertificate c) {
        SupplierQualityCertificateDto dto = modelMapper.map(c, SupplierQualityCertificateDto.class);
        if (c.getSupplierOrg() != null) dto.setSupplierOrgId(c.getSupplierOrg().getAccountId());
        if (c.getPurchaseOrder() != null) {
            dto.setPurchaseOrderId(c.getPurchaseOrder().getPurchaseOrderId());
            dto.setPoNumber(c.getPurchaseOrder().getPoNumber());
        }
        if (c.getShipment() != null) {
            dto.setShipmentId(c.getShipment().getShipmentId());
            dto.setShipmentNumber(c.getShipment().getShipmentNumber());
        }
        if (c.getCatalog() != null) {
            dto.setCatalogId(c.getCatalog().getCatalogId());
            dto.setCatalogName(c.getCatalog().getName());
        }
        return dto;
    }
}
