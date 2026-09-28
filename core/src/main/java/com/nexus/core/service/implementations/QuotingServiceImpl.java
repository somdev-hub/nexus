package com.nexus.core.service.implementations;

import com.nexus.core.dto.ShipmentDto;
import com.nexus.core.model.entities.Account;
import com.nexus.core.model.enums.FleetAssetType;
import com.nexus.core.model.entities.FreightRate;
import com.nexus.core.model.enums.QuoteStatus;
import com.nexus.core.model.enums.RateType;
import com.nexus.core.model.entities.Shipment;
import com.nexus.core.model.enums.ShipmentMode;
import com.nexus.core.model.entities.ShipmentQuote;
import com.nexus.core.model.enums.ShipmentStatus;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.FreightRateDto;
import com.nexus.core.payload.ShipmentQuoteDto;
import com.nexus.core.repository.FreightRateRepo;
import com.nexus.core.repository.ShipmentQuoteRepo;
import com.nexus.core.repository.ShipmentRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.interfaces.AccountDirectory;
import com.nexus.core.service.interfaces.QuotingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuotingServiceImpl implements QuotingService {

    private final ShipmentQuoteRepo quoteRepo;
    private final FreightRateRepo rateRepo;
    private final ShipmentRepo shipmentRepo;
    private final AccountDirectory accountDirectory;
    private final ModelMapper modelMapper;

    private static final Map<QuoteStatus, Set<QuoteStatus>> QUOTE_ALLOWED = Map.of(
            QuoteStatus.DRAFT, Set.of(QuoteStatus.SUBMITTED, QuoteStatus.CANCELLED),
            QuoteStatus.SUBMITTED, Set.of(QuoteStatus.ACCEPTED, QuoteStatus.REJECTED, QuoteStatus.EXPIRED, QuoteStatus.CANCELLED),
            QuoteStatus.ACCEPTED, Set.of(QuoteStatus.BOOKED, QuoteStatus.CANCELLED),
            QuoteStatus.BOOKED, Set.of(),
            QuoteStatus.REJECTED, Set.of(QuoteStatus.DRAFT),
            QuoteStatus.EXPIRED, Set.of(QuoteStatus.DRAFT),
            QuoteStatus.CANCELLED, Set.of());

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getLoadBoard(String mode, String search, Pageable pageable) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        ShipmentMode filterMode = null;
        if (mode != null && !mode.isBlank()) {
            try {
                filterMode = ShipmentMode.valueOf(mode.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid mode: " + mode));
            }
        }
        final ShipmentMode shipmentMode = filterMode;
        final String searchFilter = search;
        // Available loads: BOOKED/APPROVED shipments not yet assigned to this logistics org
        Page<Shipment> page = shipmentRepo.findByLogisticsOrgAccountId(orgId, pageable);
        List<ShipmentDto> mine = page.getContent().stream().map(s -> modelMapper.map(s, ShipmentDto.class)).toList();
        // Also surface open demand: shipments in BOOKED status across partnerships (org-scoped view)
        var open = shipmentRepo.findAll(pageable).stream()
                .filter(s -> s.getStatus() == ShipmentStatus.BOOKED || s.getStatus() == ShipmentStatus.APPROVED)
                .filter(s -> shipmentMode == null || s.getShipmentMode() == shipmentMode)
                .filter(s -> searchFilter == null || searchFilter.isBlank()
                        || (s.getShipmentNumber() != null && s.getShipmentNumber().toLowerCase().contains(searchFilter.toLowerCase())))
                .map(s -> modelMapper.map(s, ShipmentDto.class))
                .toList();
        var combined = new java.util.ArrayList<>(open);
        for (var dto : mine) {
            if (combined.stream().noneMatch(o -> o.getShipmentId() != null && o.getShipmentId().equals(dto.getShipmentId()))) {
                combined.add(dto);
            }
        }
        return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(combined, pageable, combined.size()));
    }

    @Override
    @Transactional
    public ResponseEntity<?> createQuote(ShipmentQuoteDto dto) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        Account org = accountDirectory.getOrCreateAccount(orgId);
        Shipment shipment = null;
        if (dto.getShipmentId() != null) {
            shipment = shipmentRepo.findById(dto.getShipmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Shipment", "shipmentId", dto.getShipmentId()));
        }
        var quote = modelMapper.map(dto, ShipmentQuote.class);
        quote.setQuoteId(null);
        quote.setLogisticsOrg(org);
        quote.setShipment(shipment);
        quote.setQuoteNumber("QT-" + System.currentTimeMillis());
        quote.setStatus(QuoteStatus.DRAFT);
        quote.setTotalAmount(totalOf(quote.getBaseRate(), quote.getFuelSurcharge(), quote.getAccessorialCharges()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toQuoteDto(quoteRepo.save(quote)));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getQuote(Long id) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var quote = quoteRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ShipmentQuote", "quoteId", id));
        return ResponseEntity.ok(toQuoteDto(quote));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllQuotes(Long shipmentId, String status, String search, Pageable pageable) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        QuoteStatus st = null;
        if (status != null && !status.isBlank()) {
            try {
                st = QuoteStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + status));
            }
        }
        Page<ShipmentQuote> page = quoteRepo.findByOrgWithFilters(orgId, shipmentId, st, blankToNull(search), pageable);
        return ResponseEntity.ok(page.map(this::toQuoteDto));
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateQuote(Long id, ShipmentQuoteDto dto) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var existing = quoteRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ShipmentQuote", "quoteId", id));
        if (existing.getStatus() != QuoteStatus.DRAFT && existing.getStatus() != QuoteStatus.REJECTED && existing.getStatus() != QuoteStatus.EXPIRED) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only DRAFT/REJECTED/EXPIRED quotes can be edited"));
        }
        var status = existing.getStatus();
        if (dto.getBaseRate() != null) existing.setBaseRate(dto.getBaseRate());
        if (dto.getFuelSurcharge() != null) existing.setFuelSurcharge(dto.getFuelSurcharge());
        if (dto.getAccessorialCharges() != null) existing.setAccessorialCharges(dto.getAccessorialCharges());
        if (dto.getAccessorialDetails() != null) existing.setAccessorialDetails(dto.getAccessorialDetails());
        if (dto.getCurrency() != null) existing.setCurrency(dto.getCurrency());
        if (dto.getValidUntil() != null) existing.setValidUntil(dto.getValidUntil());
        if (dto.getNotes() != null) existing.setNotes(dto.getNotes());
        if (dto.getShipmentId() != null && (existing.getShipment() == null
                || !dto.getShipmentId().equals(existing.getShipment().getShipmentId()))) {
            var shipment = shipmentRepo.findById(dto.getShipmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Shipment", "shipmentId", dto.getShipmentId()));
            existing.setShipment(shipment);
        }
        existing.setStatus(dto.getStatus() != null ? dto.getStatus() : status);
        existing.setTotalAmount(totalOf(existing.getBaseRate(), existing.getFuelSurcharge(), existing.getAccessorialCharges()));
        return ResponseEntity.ok(toQuoteDto(quoteRepo.save(existing)));
    }

    @Override
    @Transactional
    public ResponseEntity<?> transitionQuoteStatus(Long id, String newStatus, Map<String, Object> params) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var quote = quoteRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ShipmentQuote", "quoteId", id));
        QuoteStatus target;
        try {
            target = QuoteStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + newStatus));
        }
        if (!QUOTE_ALLOWED.getOrDefault(quote.getStatus(), Set.of()).contains(target)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid transition: " + quote.getStatus() + " -> " + target));
        }
        quote.setStatus(target);
        if (params != null && params.get("notes") != null) quote.setNotes(String.valueOf(params.get("notes")));
        if (target == QuoteStatus.BOOKED && quote.getShipment() != null) {
            var shipment = quote.getShipment();
            if (shipment.getStatus() == ShipmentStatus.BOOKED || shipment.getStatus() == ShipmentStatus.APPROVED) {
                shipment.setStatus(ShipmentStatus.ASSIGNED);
                shipment.setFreightCost(quote.getTotalAmount() != null ? quote.getTotalAmount().doubleValue() : null);
                shipmentRepo.save(shipment);
            }
        }
        return ResponseEntity.ok(toQuoteDto(quoteRepo.save(quote)));
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteQuote(Long id) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var quote = quoteRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ShipmentQuote", "quoteId", id));
        quote.setIsActive(false);
        quoteRepo.save(quote);
        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    public ResponseEntity<?> createRate(FreightRateDto dto) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        Account org = accountDirectory.getOrCreateAccount(orgId);
        rateRepo.findByRateCodeAndLogisticsOrgAccountId(dto.getRateCode(), orgId)
                .ifPresent(r -> { throw new IllegalArgumentException("Rate code already exists: " + dto.getRateCode()); });
        var rate = modelMapper.map(dto, FreightRate.class);
        rate.setRateId(null);
        rate.setLogisticsOrg(org);
        if (rate.getIsActiveRate() == null) rate.setIsActiveRate(true);
        return ResponseEntity.status(HttpStatus.CREATED).body(modelMapper.map(rateRepo.save(rate), FreightRateDto.class));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getRate(Long id) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var rate = rateRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("FreightRate", "rateId", id));
        return ResponseEntity.ok(modelMapper.map(rate, FreightRateDto.class));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllRates(String rateType, String equipmentType, String search, Pageable pageable) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        RateType rt = null;
        FleetAssetType et = null;
        try {
            if (rateType != null && !rateType.isBlank()) rt = RateType.valueOf(rateType.toUpperCase());
            if (equipmentType != null && !equipmentType.isBlank()) et = FleetAssetType.valueOf(equipmentType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid filter value"));
        }
        Page<FreightRate> page = rateRepo.findByOrgWithFilters(orgId, rt, et, blankToNull(search), pageable);
        return ResponseEntity.ok(page.map(r -> modelMapper.map(r, FreightRateDto.class)));
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateRate(Long id, FreightRateDto dto) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var existing = rateRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("FreightRate", "rateId", id));
        var org = existing.getLogisticsOrg();
        modelMapper.map(dto, existing);
        existing.setRateId(id);
        existing.setLogisticsOrg(org);
        return ResponseEntity.ok(modelMapper.map(rateRepo.save(existing), FreightRateDto.class));
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteRate(Long id) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var rate = rateRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("FreightRate", "rateId", id));
        rate.setIsActive(false);
        rate.setIsActiveRate(false);
        rateRepo.save(rate);
        return ResponseEntity.noContent().build();
    }

    private ShipmentQuoteDto toQuoteDto(ShipmentQuote quote) {
        var dto = modelMapper.map(quote, ShipmentQuoteDto.class);
        if (quote.getShipment() != null) dto.setShipmentId(quote.getShipment().getShipmentId());
        return dto;
    }

    private static BigDecimal totalOf(BigDecimal... parts) {
        var total = BigDecimal.ZERO;
        for (var part : parts) {
            if (part != null) total = total.add(part);
        }
        return total;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
