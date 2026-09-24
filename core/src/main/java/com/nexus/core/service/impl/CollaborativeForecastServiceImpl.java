package com.nexus.core.service.impl;

import com.nexus.core.entities.Account;
import com.nexus.core.entities.CollaborativeForecast;
import com.nexus.core.entities.SupplierCatalog;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.CollaborativeForecastDto;
import com.nexus.core.repository.AccountRepository;
import com.nexus.core.repository.CollaborativeForecastRepo;
import com.nexus.core.repository.SupplierCatalogRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.CollaborativeForecastService;
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
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollaborativeForecastServiceImpl implements CollaborativeForecastService {

    private final CollaborativeForecastRepo forecastRepo;
    private final SupplierCatalogRepo catalogRepo;
    private final AccountRepository accountRepo;
    private final ModelMapper modelMapper;

    private static final Map<CollaborativeForecast.ForecastStatus, Set<CollaborativeForecast.ForecastStatus>> ALLOWED = Map.of(
            CollaborativeForecast.ForecastStatus.DRAFT, Set.of(CollaborativeForecast.ForecastStatus.SHARED),
            CollaborativeForecast.ForecastStatus.SHARED, Set.of(CollaborativeForecast.ForecastStatus.CONFIRMED, CollaborativeForecast.ForecastStatus.REJECTED),
            CollaborativeForecast.ForecastStatus.CONFIRMED, Set.of(),
            CollaborativeForecast.ForecastStatus.REJECTED, Set.of(CollaborativeForecast.ForecastStatus.DRAFT)
    );

    @Override
    @Transactional
    public ResponseEntity<?> createForecast(CollaborativeForecastDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        Account supplierOrg = accountRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", orgId));
        Account retailerOrg = null;
        if (dto.getRetailerOrgId() != null) {
            retailerOrg = accountRepo.findById(dto.getRetailerOrgId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", dto.getRetailerOrgId()));
        }
        SupplierCatalog catalog = null;
        if (dto.getCatalogId() != null) {
            catalog = catalogRepo.findByCatalogIdAndSupplierOrgAccountId(dto.getCatalogId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("SupplierCatalog", "catalogId", dto.getCatalogId()));
        }
        CollaborativeForecast f = modelMapper.map(dto, CollaborativeForecast.class);
        f.setForecastId(null);
        f.setSupplierOrg(supplierOrg);
        f.setRetailerOrg(retailerOrg);
        f.setCatalog(catalog);
        if (f.getStatus() == null) f.setStatus(CollaborativeForecast.ForecastStatus.DRAFT);
        CollaborativeForecast saved = forecastRepo.save(f);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getForecast(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        CollaborativeForecast f = forecastRepo.findByForecastIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("CollaborativeForecast", "forecastId", id));
        return ResponseEntity.ok(mapToDto(f));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllForecasts(Long retailerOrgId, Long catalogId, String status, Date periodStart, Date periodEnd, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        CollaborativeForecast.ForecastStatus st = null;
        if (status != null && !status.isBlank()) {
            try { st = CollaborativeForecast.ForecastStatus.valueOf(status.toUpperCase()); }
            catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + status)); }
        }
        Page<CollaborativeForecast> page = forecastRepo.findByOrgWithFilters(orgId, retailerOrgId, catalogId, st, periodStart, periodEnd, pageable);
        Page<CollaborativeForecastDto> dtoPage = page.map(this::mapToDto);
        return ResponseEntity.ok(dtoPage);
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateForecast(Long id, CollaborativeForecastDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        CollaborativeForecast f = forecastRepo.findByForecastIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("CollaborativeForecast", "forecastId", id));
        if (f.getStatus() != CollaborativeForecast.ForecastStatus.DRAFT && f.getStatus() != CollaborativeForecast.ForecastStatus.REJECTED) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only DRAFT or REJECTED forecasts can be updated"));
        }
        if (dto.getPeriodStart() != null) f.setPeriodStart(dto.getPeriodStart());
        if (dto.getPeriodEnd() != null) f.setPeriodEnd(dto.getPeriodEnd());
        if (dto.getForecastQuantity() != null) f.setForecastQuantity(dto.getForecastQuantity());
        if (dto.getConfidencePct() != null) f.setConfidencePct(dto.getConfidencePct());
        if (dto.getNotes() != null) f.setNotes(dto.getNotes());
        CollaborativeForecast saved = forecastRepo.save(f);
        return ResponseEntity.ok(mapToDto(saved));
    }

    @Override
    @Transactional
    public ResponseEntity<?> transitionStatus(Long id, String newStatus) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        CollaborativeForecast f = forecastRepo.findByForecastIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("CollaborativeForecast", "forecastId", id));
        CollaborativeForecast.ForecastStatus target;
        try { target = CollaborativeForecast.ForecastStatus.valueOf(newStatus.toUpperCase()); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + newStatus)); }
        if (!ALLOWED.getOrDefault(f.getStatus(), Set.of()).contains(target)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Transition not allowed: " + f.getStatus() + " -> " + target));
        }
        f.setStatus(target);
        CollaborativeForecast saved = forecastRepo.save(f);
        return ResponseEntity.ok(mapToDto(saved));
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteForecast(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        CollaborativeForecast f = forecastRepo.findByForecastIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("CollaborativeForecast", "forecastId", id));
        if (f.getStatus() == CollaborativeForecast.ForecastStatus.CONFIRMED) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot delete CONFIRMED forecast"));
        }
        forecastRepo.delete(f);
        return ResponseEntity.noContent().build();
    }

    private CollaborativeForecastDto mapToDto(CollaborativeForecast f) {
        CollaborativeForecastDto dto = modelMapper.map(f, CollaborativeForecastDto.class);
        if (f.getSupplierOrg() != null) dto.setSupplierOrgId(f.getSupplierOrg().getAccountId());
        if (f.getRetailerOrg() != null) {
            dto.setRetailerOrgId(f.getRetailerOrg().getAccountId());
            dto.setRetailerOrgName(f.getRetailerOrg().getName());
        }
        if (f.getCatalog() != null) {
            dto.setCatalogId(f.getCatalog().getCatalogId());
            dto.setCatalogName(f.getCatalog().getName());
        }
        return dto;
    }
}
