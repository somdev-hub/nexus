package com.nexus.core.service.impl;

import com.nexus.core.entities.SupplierCatalog;
import com.nexus.core.entities.SupplierContract;
import com.nexus.core.entities.SupplierPriceTier;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.SupplierPriceTierDto;
import com.nexus.core.repository.SupplierCatalogRepo;
import com.nexus.core.repository.SupplierContractRepo;
import com.nexus.core.repository.SupplierPriceTierRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.SupplierPriceTierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.Comparator;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierPriceTierServiceImpl implements SupplierPriceTierService {

    private final SupplierPriceTierRepo tierRepo;
    private final SupplierCatalogRepo catalogRepo;
    private final SupplierContractRepo contractRepo;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public ResponseEntity<?> createPriceTier(SupplierPriceTierDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierCatalog catalog = catalogRepo.findByCatalogIdAndSupplierOrgAccountId(dto.getCatalogId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierCatalog", "catalogId", dto.getCatalogId()));
        SupplierPriceTier tier = modelMapper.map(dto, SupplierPriceTier.class);
        tier.setTierId(null);
        tier.setCatalog(catalog);
        if (dto.getContractId() != null) {
            SupplierContract contract = contractRepo.findById(dto.getContractId())
                    .filter(c -> c.getAccount().getAccountId().equals(orgId))
                    .orElseThrow(() -> new ResourceNotFoundException("SupplierContract", "contractId", dto.getContractId()));
            tier.setContract(contract);
        }
        SupplierPriceTier saved = tierRepo.save(tier);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getPriceTier(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierPriceTier tier = tierRepo.findByTierIdAndCatalogSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierPriceTier", "tierId", id));
        return ResponseEntity.ok(mapToDto(tier));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllPriceTiers(Long catalogId, String customerSegment, Long contractId, Date validFrom, Date validTo, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        Page<SupplierPriceTier> page = tierRepo.findByOrgWithFilters(orgId, catalogId, customerSegment, contractId, validFrom, validTo, pageable);
        Page<SupplierPriceTierDto> dtoPage = page.map(this::mapToDto);
        return ResponseEntity.ok(dtoPage);
    }

    @Override
    @Transactional
    public ResponseEntity<?> updatePriceTier(Long id, SupplierPriceTierDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierPriceTier tier = tierRepo.findByTierIdAndCatalogSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierPriceTier", "tierId", id));
        if (dto.getTierName() != null) tier.setTierName(dto.getTierName());
        if (dto.getMinQuantity() != null) tier.setMinQuantity(dto.getMinQuantity());
        if (dto.getMaxQuantity() != null) tier.setMaxQuantity(dto.getMaxQuantity());
        if (dto.getUnitPrice() != null) tier.setUnitPrice(dto.getUnitPrice());
        if (dto.getCustomerSegment() != null) tier.setCustomerSegment(dto.getCustomerSegment());
        if (dto.getCurrency() != null) tier.setCurrency(dto.getCurrency());
        if (dto.getValidFrom() != null) tier.setValidFrom(dto.getValidFrom());
        if (dto.getValidTo() != null) tier.setValidTo(dto.getValidTo());
        SupplierPriceTier saved = tierRepo.save(tier);
        return ResponseEntity.ok(mapToDto(saved));
    }

    @Override
    @Transactional
    public ResponseEntity<?> deletePriceTier(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierPriceTier tier = tierRepo.findByTierIdAndCatalogSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierPriceTier", "tierId", id));
        tierRepo.delete(tier);
        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getPriceForQuantity(Long catalogId, Double quantity, String customerSegment) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        // consolidated pricing resolution: volume + segment + validity
        var tiers = tierRepo.findByOrgWithFilters(orgId, catalogId, customerSegment, null, null, null, Pageable.unpaged()).getContent();
        // filter by quantity and validity date
        Date today = new Date(System.currentTimeMillis());
        var applicable = tiers.stream()
                .filter(t -> quantity == null || (t.getMinQuantity() == null || quantity >= t.getMinQuantity()) && (t.getMaxQuantity() == null || quantity <= t.getMaxQuantity()))
                .filter(t -> (t.getValidFrom() == null || !today.before(t.getValidFrom())) && (t.getValidTo() == null || !today.after(t.getValidTo())))
                .min(Comparator.comparing(SupplierPriceTier::getUnitPrice));
        if (applicable.isEmpty()) {
            // fallback: any tier for catalog sorted by minQuantity
            var fallback = tiers.stream()
                    .filter(t -> t.getCustomerSegment() == null || t.getCustomerSegment().equals(customerSegment))
                    .min(Comparator.comparing(t -> t.getMinQuantity() != null ? t.getMinQuantity() : 0.0));
            if (fallback.isPresent()) return ResponseEntity.ok(mapToDto(fallback.get()));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "No price tier found for quantity " + quantity));
        }
        return ResponseEntity.ok(mapToDto(applicable.get()));
    }

    private SupplierPriceTierDto mapToDto(SupplierPriceTier t) {
        SupplierPriceTierDto dto = modelMapper.map(t, SupplierPriceTierDto.class);
        if (t.getCatalog() != null) {
            dto.setCatalogId(t.getCatalog().getCatalogId());
            dto.setCatalogName(t.getCatalog().getName());
        }
        if (t.getContract() != null) dto.setContractId(t.getContract().getContractId());
        return dto;
    }
}
