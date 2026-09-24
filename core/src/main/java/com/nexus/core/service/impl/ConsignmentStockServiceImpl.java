package com.nexus.core.service.impl;

import com.nexus.core.entities.Account;
import com.nexus.core.entities.ConsignmentStock;
import com.nexus.core.entities.Material;
import com.nexus.core.entities.Warehouse;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.ConsignmentStockDto;
import com.nexus.core.repository.AccountRepository;
import com.nexus.core.repository.ConsignmentStockRepo;
import com.nexus.core.repository.MaterialRepo;
import com.nexus.core.repository.WarehouseRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.ConsignmentStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsignmentStockServiceImpl implements ConsignmentStockService {

    private final ConsignmentStockRepo stockRepo;
    private final AccountRepository accountRepo;
    private final WarehouseRepo warehouseRepo;
    private final MaterialRepo materialRepo;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public ResponseEntity<?> createConsignment(ConsignmentStockDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        Account supplierOrg = accountRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", orgId));
        Account retailerOrg = accountRepo.findById(dto.getRetailerOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", dto.getRetailerOrgId()));
        Material material = materialRepo.findByMaterialIdAndOrg(dto.getMaterialId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Material", "materialId", dto.getMaterialId()));
        Warehouse warehouse = null;
        if (dto.getWarehouseId() != null) {
            warehouse = warehouseRepo.findById(dto.getWarehouseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "warehouseId", dto.getWarehouseId()));
        }
        ConsignmentStock cs = new ConsignmentStock();
        cs.setSupplierOrg(supplierOrg);
        cs.setRetailerOrg(retailerOrg);
        cs.setWarehouse(warehouse);
        cs.setMaterial(material);
        cs.setQuantityOnHand(dto.getQuantityOnHand() != null ? dto.getQuantityOnHand() : 0.0);
        cs.setQuantityReserved(dto.getQuantityReserved() != null ? dto.getQuantityReserved() : 0.0);
        cs.recalculateAvailable();
        cs.setConsignmentNumber("CONS-" + System.currentTimeMillis());
        cs.setNotes(dto.getNotes());
        ConsignmentStock saved = stockRepo.save(cs);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getConsignment(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        ConsignmentStock cs = stockRepo.findByConsignmentIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ConsignmentStock", "consignmentId", id));
        return ResponseEntity.ok(mapToDto(cs));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllConsignments(Long retailerOrgId, Long warehouseId, Long materialId, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        Page<ConsignmentStock> page = stockRepo.findByOrgWithFilters(orgId, retailerOrgId, warehouseId, materialId, pageable);
        Page<ConsignmentStockDto> dtoPage = page.map(this::mapToDto);
        return ResponseEntity.ok(dtoPage);
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateConsignment(Long id, ConsignmentStockDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        ConsignmentStock cs = stockRepo.findByConsignmentIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ConsignmentStock", "consignmentId", id));
        if (dto.getQuantityOnHand() != null) cs.setQuantityOnHand(dto.getQuantityOnHand());
        if (dto.getQuantityReserved() != null) cs.setQuantityReserved(dto.getQuantityReserved());
        if (dto.getNotes() != null) cs.setNotes(dto.getNotes());
        cs.recalculateAvailable();
        ConsignmentStock saved = stockRepo.save(cs);
        return ResponseEntity.ok(mapToDto(saved));
    }

    @Override
    @Transactional
    public ResponseEntity<?> adjustQuantity(Long id, Double quantity, String reason) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        ConsignmentStock cs = stockRepo.findByConsignmentIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ConsignmentStock", "consignmentId", id));
        cs.setQuantityOnHand(cs.getQuantityOnHand() + quantity);
        cs.recalculateAvailable();
        ConsignmentStock saved = stockRepo.save(cs);
        log.info("Adjusted consignment {} by {} reason {}", id, quantity, reason);
        return ResponseEntity.ok(mapToDto(saved));
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteConsignment(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        ConsignmentStock cs = stockRepo.findByConsignmentIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ConsignmentStock", "consignmentId", id));
        stockRepo.delete(cs);
        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getSummary() {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        var all = stockRepo.findByOrgWithFilters(orgId, null, null, null, Pageable.unpaged()).getContent();
        double totalOnHand = all.stream().mapToDouble(cs -> cs.getQuantityOnHand() != null ? cs.getQuantityOnHand() : 0).sum();
        double totalAvail = all.stream().mapToDouble(cs -> cs.getQuantityAvailable() != null ? cs.getQuantityAvailable() : 0).sum();
        return ResponseEntity.ok(Map.of("totalConsignments", all.size(), "totalOnHand", totalOnHand, "totalAvailable", totalAvail));
    }

    private ConsignmentStockDto mapToDto(ConsignmentStock cs) {
        ConsignmentStockDto dto = modelMapper.map(cs, ConsignmentStockDto.class);
        if (cs.getSupplierOrg() != null) dto.setSupplierOrgId(cs.getSupplierOrg().getAccountId());
        if (cs.getRetailerOrg() != null) {
            dto.setRetailerOrgId(cs.getRetailerOrg().getAccountId());
            dto.setRetailerOrgName(cs.getRetailerOrg().getName());
        }
        if (cs.getWarehouse() != null) {
            dto.setWarehouseId(cs.getWarehouse().getWarehouseId());
            dto.setWarehouseCode(cs.getWarehouse().getCode());
        }
        if (cs.getMaterial() != null) {
            dto.setMaterialId(cs.getMaterial().getMaterialId());
            dto.setMaterialName(cs.getMaterial().getName());
        }
        return dto;
    }
}
