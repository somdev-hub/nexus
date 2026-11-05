package com.nexus.core.service.implementations;

import com.nexus.core.model.entities.Account;
import com.nexus.core.model.entities.ConsignmentStock;
import com.nexus.core.dto.ShipmentDto;
import com.nexus.core.dto.ShipmentDto;
import com.nexus.core.model.entities.ConsignmentConsumption;
import com.nexus.core.model.entities.Material;
import com.nexus.core.model.entities.ProductVariant;
import com.nexus.core.model.enums.ShipmentMode;
import com.nexus.core.model.enums.ShipmentStatus;
import com.nexus.core.model.entities.Stock;
import com.nexus.core.model.entities.Supplier;
import com.nexus.core.model.entities.SupplierCatalog;
import com.nexus.core.model.entities.SupplierContract;
import com.nexus.core.model.entities.Warehouse;
import com.nexus.core.model.enums.ShipmentMode;
import com.nexus.core.model.enums.ShipmentStatus;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.exception.ValidationException;
import com.nexus.core.payload.ConsignmentStockDto;
import com.nexus.core.repository.AccountRepository;
import com.nexus.core.repository.ConsignmentStockRepo;
import com.nexus.core.repository.ConsignmentConsumptionRepo;
import com.nexus.core.repository.MaterialRepo;
import com.nexus.core.repository.ProductVariantRepo;
import com.nexus.core.repository.StockRepo;
import com.nexus.core.repository.SupplierCatalogRepo;
import com.nexus.core.repository.SupplierContractRepo;
import com.nexus.core.repository.SupplierPriceTierRepo;
import com.nexus.core.repository.SupplierRepository;
import com.nexus.core.repository.WarehouseRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.interfaces.ConsignmentStockService;
import com.nexus.core.service.interfaces.ShipmentService;
import com.nexus.core.service.interfaces.ShipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsignmentStockServiceImpl implements ConsignmentStockService {

    private final ConsignmentStockRepo stockRepo;
    private final AccountRepository accountRepo;
    private final WarehouseRepo warehouseRepo;
    private final MaterialRepo materialRepo;
    private final StockRepo stockRepo2;
    private final ConsignmentConsumptionRepo consumptionRepo;
    private final SupplierCatalogRepo supplierCatalogRepo;
    private final ShipmentService shipmentService;
    private final SupplierRepository supplierRepo;
    private final ProductVariantRepo productVariantRepo;
    private final SupplierPriceTierRepo supplierPriceTierRepo;
    private final SupplierContractRepo supplierContractRepo;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public ResponseEntity<?> createConsignment(ConsignmentStockDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        Account supplierOrg = accountRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", orgId));
        Account retailerOrg = accountRepo.findById(dto.getRetailerOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", dto.getRetailerOrgId()));

        boolean hasConsignmentContract = supplierContractRepo
                .findByAccountAccountIdAndContractType(retailerOrg.getAccountId(), SupplierContract.ContractType.CONSIGNMENT, Pageable.unpaged())
                .getContent().stream().anyMatch(c -> c.getStatus() == SupplierContract.ContractStatus.ACTIVE
                        && c.getSupplier() != null
                        && orgId.equals(c.getSupplier().getSupplierOrgAccountId()));
        if (!hasConsignmentContract) {
            throw new ValidationException("Active CONSIGNMENT contract required between supplier and retailer");
        }
        Material material = null;
        if (dto.getCatalogId() != null) {
            SupplierCatalog catalog = supplierCatalogRepo.findById(dto.getCatalogId()).orElse(null);
            if (catalog != null) {
                material = materialRepo.findFirstByOrgAndCatalogId(orgId, catalog.getCatalogId()).orElse(null);
                if (material == null) {
                    material = new Material();
                    material.setOrg(orgId);
                    material.setCatalogId(catalog.getCatalogId());
                    material.setName(catalog.getName());
                    material.setCode(catalog.getCode());
                    material.setCategory(catalog.getCategory() != null ? catalog.getCategory() : catalog.getFamily());
                    material.setUnit(catalog.getUnitOfMeasure());
                    material.setCurrency(catalog.getCurrency() != null ? catalog.getCurrency() : "USD");
                    material.setPricePerUnit(catalog.getBasePrice());
                    material.setSupplierOrgId(catalog.getSupplierOrg() != null ? catalog.getSupplierOrg().getAccountId() : null);
                    material = materialRepo.save(material);
                }
            }
        }
        if (material == null && dto.getMaterialId() != null) {
            material = materialRepo.findByMaterialIdAndOrg(dto.getMaterialId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Material", "materialId", dto.getMaterialId()));
        }
        if (material == null) {
            throw new ValidationException("Catalog item is required");
        }
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
        cs.setCatalogId(dto.getCatalogId());
        cs.setVariantId(dto.getVariantId());
        cs.setPriceTierId(dto.getPriceTierId());
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
    @Transactional(readOnly = true)
    public ResponseEntity<?> getRetailerConsignments(Long warehouseId, Long materialId, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        Page<ConsignmentStock> page = stockRepo.findByRetailerOrgWithFilters(orgId, warehouseId, materialId, pageable);
        Page<ConsignmentStockDto> dtoPage = page.map(this::mapToDto);
        return ResponseEntity.ok(dtoPage);
    }

    @Override
    @Transactional
    public ResponseEntity<?> consumeConsignmentByRetailer(Long id, Double quantity) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        ConsignmentStock cs = stockRepo.findByConsignmentIdAndRetailerOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ConsignmentStock", "consignmentId", id));
        if (quantity == null || quantity <= 0) throw new ValidationException("Quantity must be positive");
        if (cs.getQuantityAvailable() == null || cs.getQuantityAvailable() < quantity)
            throw new ValidationException("Insufficient consignment stock");
        cs.setQuantityOnHand(cs.getQuantityOnHand() - quantity);
        cs.setQuantityReserved(cs.getQuantityReserved());
        cs.recalculateAvailable();
        stockRepo.save(cs);

        // Record consumption for billing/finance
        ConsignmentConsumption cc = new ConsignmentConsumption();
        cc.setConsignment(cs);
        cc.setRetailerOrg(accountRepo.findById(orgId).orElse(null));
        cc.setSupplierOrg(cs.getSupplierOrg());
        cc.setMaterial(cs.getMaterial());
        cc.setQuantity(quantity);
        double unitPrice = cs.getMaterial() != null && cs.getMaterial().getPricePerUnit() != null ? cs.getMaterial().getPricePerUnit() : 0.0;
        cc.setUnitPrice(unitPrice);
        cc.setTotalAmount(unitPrice * quantity);
        cc.setConsumedAt(Timestamp.valueOf(LocalDateTime.now()));
        cc.setConsumedBy("retailer");
        consumptionRepo.save(cc);

        // Move consumed quantity into retailer owned stock. Resolve the
        // retailer-side Material (by catalog) instead of the supplier's
        // material, so stock is tracked under the retailer org.
        Long catalogId = cs.getCatalogId();
        if (catalogId == null && cs.getMaterial() != null) catalogId = cs.getMaterial().getCatalogId();
        Material retailerMaterial = null;
        if (catalogId != null) {
            retailerMaterial = materialRepo.findFirstByOrgAndCatalogId(orgId, catalogId).orElse(null);
            if (retailerMaterial == null) {
                SupplierCatalog catalog = supplierCatalogRepo.findById(catalogId).orElse(null);
                if (catalog != null) {
                    retailerMaterial = new Material();
                    retailerMaterial.setOrg(orgId);
                    retailerMaterial.setCatalogId(catalog.getCatalogId());
                    retailerMaterial.setName(catalog.getName());
                    retailerMaterial.setCode(catalog.getCode());
                    retailerMaterial.setCategory(catalog.getCategory() != null ? catalog.getCategory() : catalog.getFamily());
                    retailerMaterial.setUnit(catalog.getUnitOfMeasure());
                    retailerMaterial.setCurrency(catalog.getCurrency() != null ? catalog.getCurrency() : "USD");
                    retailerMaterial.setPricePerUnit(catalog.getBasePrice());
                    retailerMaterial.setSupplierOrgId(catalog.getSupplierOrg() != null ? catalog.getSupplierOrg().getAccountId() : null);
                    Warehouse wh0 = warehouseRepo.findByOrg(orgId, Pageable.unpaged()).getContent().stream()
                            .filter(w -> "RESERVE".equalsIgnoreCase(w.getWarehouseType()))
                            .min(java.util.Comparator.comparingInt(w -> w.getFillOrder() != null ? w.getFillOrder() : Integer.MAX_VALUE))
                            .orElse(null);
                    if (wh0 != null) retailerMaterial.setWarehouse(wh0);
                    retailerMaterial = materialRepo.save(retailerMaterial);
                }
            }
        }
        if (retailerMaterial != null) {
            Stock st = stockRepo2.findActiveByMaterial(retailerMaterial).stream().findFirst().orElse(null);
            if (st == null) {
                Warehouse wh = warehouseRepo.findByOrg(orgId, Pageable.unpaged()).getContent().stream()
                        .filter(w -> "RESERVE".equalsIgnoreCase(w.getWarehouseType()))
                        .min(java.util.Comparator.comparingInt(w -> w.getFillOrder() != null ? w.getFillOrder() : Integer.MAX_VALUE))
                        .orElse(null);
                if (wh != null) {
                    st = new Stock();
                    st.setMaterial(retailerMaterial);
                    st.setWarehouse(wh);
                    st.setQuantityOnHand(0.0);
                    st.setQuantityReserved(0.0);
                    st.setQuantityAvailable(0.0);
                }
            }
            if (st != null) {
                st.setQuantityOnHand((st.getQuantityOnHand() != null ? st.getQuantityOnHand() : 0.0) + quantity);
                st.recalculateAvailable();
                stockRepo2.save(st);
            }
        }
        return ResponseEntity.ok(mapToDto(cs));
    }

    @Override
    @Transactional
    public ResponseEntity<?> returnConsignmentByRetailer(Long id, Double quantity) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        ConsignmentStock cs = stockRepo.findByConsignmentIdAndRetailerOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ConsignmentStock", "consignmentId", id));
        if (quantity == null || quantity <= 0) throw new ValidationException("Quantity must be positive");
        if (cs.getQuantityOnHand() == null || cs.getQuantityOnHand() < quantity)
            throw new ValidationException("Insufficient consignment stock to return");
        cs.setQuantityOnHand(cs.getQuantityOnHand() - quantity);
        cs.recalculateAvailable();
        stockRepo.save(cs);
        return ResponseEntity.ok(mapToDto(cs));
    }

    @Override
    @Transactional
    public ResponseEntity<?> receiveConsignmentByRetailer(Long id, Double quantity) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        ConsignmentStock cs = stockRepo.findByConsignmentIdAndRetailerOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ConsignmentStock", "consignmentId", id));
        if (quantity == null || quantity <= 0) throw new ValidationException("Quantity must be positive");
        cs.setQuantityOnHand((cs.getQuantityOnHand() != null ? cs.getQuantityOnHand() : 0.0) + quantity);
        cs.recalculateAvailable();
        stockRepo.save(cs);
        return ResponseEntity.ok(mapToDto(cs));
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateConsignment(Long id, ConsignmentStockDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        ConsignmentStock cs = stockRepo.findByConsignmentIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ConsignmentStock", "consignmentId", id));
        if (dto.getCatalogId() != null) {
            cs.setCatalogId(dto.getCatalogId());
            SupplierCatalog catalog = supplierCatalogRepo.findById(dto.getCatalogId()).orElse(null);
            if (catalog != null) {
                Material material = materialRepo.findFirstByOrgAndCatalogId(orgId, catalog.getCatalogId()).orElse(null);
                if (material == null) {
                    material = new Material();
                    material.setOrg(orgId);
                    material.setCatalogId(catalog.getCatalogId());
                    material.setName(catalog.getName());
                    material.setCode(catalog.getCode());
                    material.setCategory(catalog.getCategory() != null ? catalog.getCategory() : catalog.getFamily());
                    material.setUnit(catalog.getUnitOfMeasure());
                    material.setCurrency(catalog.getCurrency() != null ? catalog.getCurrency() : "USD");
                    material.setPricePerUnit(catalog.getBasePrice());
                    material.setSupplierOrgId(catalog.getSupplierOrg() != null ? catalog.getSupplierOrg().getAccountId() : null);
                    material = materialRepo.save(material);
                }
                cs.setMaterial(material);
            }
        }
        if (dto.getVariantId() != null) cs.setVariantId(dto.getVariantId());
        if (dto.getPriceTierId() != null) cs.setPriceTierId(dto.getPriceTierId());
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

    @Override
    @Transactional
    public ResponseEntity<?> fillConsignment(Long id, String pickupAddress, String deliveryAddress, Double quantity) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        ConsignmentStock cs = stockRepo.findByConsignmentIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ConsignmentStock", "consignmentId", id));
        try {
            Supplier supplier = supplierRepo.findFirstBySupplierOrgAccountId(orgId).orElse(null);
            if (supplier == null) {
                supplier = supplierRepo.findByAccountAccountId(orgId, Pageable.unpaged()).getContent().stream().findFirst().orElse(null);
            }
            if (supplier == null) {
                throw new ValidationException("Supplier not found for this organization");
            }
            {
                ShipmentDto shipment = new ShipmentDto();
                shipment.setShipmentNumber("CONS-FILL-" + id + "-" + System.currentTimeMillis());
                shipment.setReferenceType("CONSIGNMENT_FILL");
                shipment.setReferenceId(id);
                shipment.setScheduledPickupDate(java.time.LocalDateTime.now());
                shipment.setScheduledDeliveryDate(java.time.LocalDateTime.now().plusDays(3));
                shipment.setCarrierTrackingNumber(shipment.getShipmentNumber());
                shipment.setSupplierId(supplier.getSupplierId());
                shipment.setSupplierName(supplier.getBusinessName());
                Warehouse deliveryWh = cs.getWarehouse();
                if (deliveryWh == null && cs.getRetailerOrg() != null) {
                    deliveryWh = warehouseRepo.findByOrg(cs.getRetailerOrg().getAccountId(), Pageable.unpaged()).getContent().stream().findFirst().orElse(null);
                }
                if (deliveryWh == null) {
                    // create default retailer warehouse
                    deliveryWh = new Warehouse();
                    deliveryWh.setOrg(cs.getRetailerOrg().getAccountId());
                    deliveryWh.setCode("MAIN");
                    deliveryWh.setLocation("Default");
                    deliveryWh = warehouseRepo.save(deliveryWh);
                }
                shipment.setWarehouseId(deliveryWh.getWarehouseId());
                shipment.setWarehouseName(deliveryWh.getCode());
                shipment.setMode(ShipmentMode.ROAD);
                shipment.setStatus(ShipmentStatus.DRAFT);
                shipment.setSupplierOrgId(orgId);
                if (cs.getRetailerOrg() != null) {
                    shipment.setRetailerOrgId(cs.getRetailerOrg().getAccountId());
                }
                if (cs.getMaterial() != null) {
                    shipment.setMaterialId(cs.getMaterial().getMaterialId());
                    shipment.setQuantityUom(cs.getMaterial().getUnit());
                }
                if (quantity != null && quantity > 0) {
                    shipment.setShippedQuantity(quantity);
                }
                shipment.setPickupAddress(pickupAddress != null && !pickupAddress.isBlank() ? pickupAddress : (supplier.getLocation() != null ? supplier.getLocation() : "Supplier warehouse"));
                shipment.setDeliveryAddress(deliveryAddress != null && !deliveryAddress.isBlank() ? deliveryAddress : (cs.getWarehouse() != null && cs.getWarehouse().getLocation() != null ? cs.getWarehouse().getLocation() : "Retailer warehouse"));
                ShipmentDto created = shipmentService.createShipment(shipment);
                cs.setShipmentId(created.getShipmentId());
                stockRepo.save(cs);
                log.info("Created consignment fill shipment {} for consignment {}", created.getShipmentId(), id);
            }
        } catch (Exception e) {
            log.error("Failed to create consignment fill shipment", e);
            throw new ValidationException("Failed to create shipment: " + e.getMessage());
        }
        return ResponseEntity.ok(mapToDto(cs));
    }

    private ConsignmentStockDto mapToDto(ConsignmentStock cs) {
        ConsignmentStockDto dto = new ConsignmentStockDto();
        dto.setConsignmentId(cs.getConsignmentId());
        dto.setConsignmentNumber(cs.getConsignmentNumber());
        dto.setNotes(cs.getNotes());
        dto.setQuantityOnHand(cs.getQuantityOnHand());
        dto.setQuantityReserved(cs.getQuantityReserved());
        dto.setQuantityAvailable(cs.getQuantityAvailable());
        dto.setCreatedAt(cs.getCreatedAt());
        dto.setUpdatedAt(cs.getUpdatedAt());
        if (cs.getSupplierOrg() != null) {
            dto.setSupplierOrgId(cs.getSupplierOrg().getAccountId());
            dto.setSupplierOrgName(cs.getSupplierOrg().getName());
        }
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
        dto.setCatalogId(cs.getCatalogId());
        dto.setVariantId(cs.getVariantId());
        dto.setPriceTierId(cs.getPriceTierId());
        if (cs.getCatalogId() != null) {
            SupplierCatalog cat = supplierCatalogRepo.findById(cs.getCatalogId()).orElse(null);
            if (cat != null) {
                if (dto.getMaterialName() == null) dto.setMaterialName(cat.getName());
                dto.setCatalogName(cat.getName());
            }
        }
        if (cs.getVariantId() != null) {
            ProductVariant v = productVariantRepo.findById(cs.getVariantId()).orElse(null);
            if (v != null) dto.setVariantValue(v.getVariantValue());
        }
        if (cs.getVariantId() != null) {
            ProductVariant v = productVariantRepo.findById(cs.getVariantId()).orElse(null);
            if (v != null) dto.setVariantValue(v.getVariantValue());
        }
        if (cs.getPriceTierId() != null) {
            com.nexus.core.model.entities.SupplierPriceTier t = supplierPriceTierRepo.findById(cs.getPriceTierId()).orElse(null);
            if (t != null) dto.setPriceTierName(t.getTierName());
        }
        return dto;
    }
}
