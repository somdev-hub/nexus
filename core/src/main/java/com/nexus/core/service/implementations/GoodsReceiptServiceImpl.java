package com.nexus.core.service.implementations;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.core.model.entities.GoodsReceipt;
import com.nexus.core.model.entities.GoodsReceiptLineItem;
import com.nexus.core.model.enums.GoodsReceiptStatus;
import com.nexus.core.model.entities.AdvanceShipmentNotice;
import com.nexus.core.model.entities.Material;
import com.nexus.core.model.entities.PurchaseOrder;
import com.nexus.core.model.entities.PurchaseOrderLineItem;
import com.nexus.core.model.enums.PurchaseOrderStatus;
import com.nexus.core.model.entities.Shipment;
import com.nexus.core.model.entities.Stock;
import com.nexus.core.model.entities.Supplier;
import com.nexus.core.model.entities.SupplierCatalog;
import com.nexus.core.model.entities.Warehouse;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.exception.ValidationException;
import com.nexus.core.payload.GoodsReceiptDto;
import com.nexus.core.payload.GoodsReceiptLineItemDto;
import com.nexus.core.repository.AdvanceShipmentNoticeRepo;
import com.nexus.core.repository.GoodsReceiptLineItemRepo;
import com.nexus.core.repository.GoodsReceiptRepo;
import com.nexus.core.repository.MaterialRepo;
import com.nexus.core.repository.PurchaseOrderLineItemRepo;
import com.nexus.core.repository.PurchaseOrderRepo;
import com.nexus.core.repository.ShipmentRepo;
import com.nexus.core.repository.StockRepo;
import com.nexus.core.repository.SupplierCatalogRepo;
import com.nexus.core.repository.SupplierRepository;
import com.nexus.core.repository.WarehouseRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.interfaces.GoodsReceiptService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoodsReceiptServiceImpl implements GoodsReceiptService {

	private final GoodsReceiptRepo goodsReceiptRepo;
	private final GoodsReceiptLineItemRepo grLineItemRepo;
	private final PurchaseOrderRepo purchaseOrderRepo;
	private final PurchaseOrderLineItemRepo poLineItemRepo;
	private final ShipmentRepo shipmentRepo;
	private final AdvanceShipmentNoticeRepo asnRepo;
	private final SupplierRepository supplierRepo;
	private final MaterialRepo materialRepo;
	private final StockRepo stockRepo;
	private final WarehouseRepo warehouseRepo;
	private final SupplierCatalogRepo supplierCatalogRepo;

	@Override
	@Transactional
	public ResponseEntity<?> createGoodsReceipt(GoodsReceiptDto grDto) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		boolean shipmentSource = grDto.getShipmentId() != null;

		PurchaseOrder po = null;
		Supplier supplier;
		Shipment shipment = null;

		if (shipmentSource) {
			shipment = shipmentRepo.findById(grDto.getShipmentId())
					.orElseThrow(() -> new ResourceNotFoundException("Shipment", "shipmentId",
							grDto.getShipmentId()));
			if (shipment.getRetailerOrg() == null
					|| !shipment.getRetailerOrg().getAccountId().equals(orgId)) {
				throw new ResourceNotFoundException("Shipment", "shipmentId", grDto.getShipmentId());
			}
			if (!isCoreReference(shipment.getReferenceType())) {
				throw new ValidationException(
						"Only consignment/VMI core shipments can be received without a purchase order");
			}
			if (shipment.getStatus() != com.nexus.core.model.enums.ShipmentStatus.DELIVERED
					&& shipment.getStatus() != com.nexus.core.model.enums.ShipmentStatus.PARTIALLY_DELIVERED
					&& shipment.getStatus() != com.nexus.core.model.enums.ShipmentStatus.CLOSED) {
				throw new ValidationException(
						"Shipment must be delivered before goods receipt. Current: " + shipment.getStatus());
			}
			boolean existing = goodsReceiptRepo
					.findByShipmentShipmentId(shipment.getShipmentId(), Pageable.unpaged())
					.getContent().stream()
					.anyMatch(g -> g.getStatus() != GoodsReceiptStatus.CANCELLED);
			if (existing) {
				throw new ValidationException("A goods receipt already exists for this shipment");
			}
			if (shipment.getSupplierOrg() == null) {
				throw new ValidationException("Shipment has no supplier organization");
			}
			Long supplierOrgId = shipment.getSupplierOrg().getAccountId();
			supplier = supplierRepo.findFirstBySupplierOrgAccountId(supplierOrgId)
					.orElseGet(() -> supplierRepo
							.findByAccountAccountId(supplierOrgId, Pageable.unpaged())
							.getContent().stream().findFirst().orElse(null));
			if (supplier == null) {
				throw new ResourceNotFoundException("Supplier", "supplierOrgId", supplierOrgId);
			}
			if (grDto.getSupplierId() != null
					&& !supplier.getSupplierId().equals(grDto.getSupplierId())) {
				throw new ValidationException("Supplier does not match the shipment supplier");
			}
		} else {
			if (grDto.getPurchaseOrderId() == null) {
				throw new ValidationException("Purchase order or shipment is required");
			}
			po = purchaseOrderRepo
					.findByPurchaseOrderIdAndBuyerOrgAccountId(grDto.getPurchaseOrderId(), orgId)
					.orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "purchaseOrderId",
							grDto.getPurchaseOrderId()));
			supplier = supplierRepo
					.findBySupplierIdAndAccountAccountId(grDto.getSupplierId(), orgId)
					.orElseThrow(() -> new ResourceNotFoundException("Supplier", "supplierId",
							grDto.getSupplierId()));
			if (!supplier.getSupplierId().equals(po.getSupplier().getSupplierId())) {
				throw new ValidationException("Supplier does not match the purchase order supplier");
			}
			if (po.getStatus() == PurchaseOrderStatus.DRAFT
					|| po.getStatus() == PurchaseOrderStatus.PENDING_APPROVAL
					|| po.getStatus() == PurchaseOrderStatus.REJECTED
					|| po.getStatus() == PurchaseOrderStatus.CANCELLED) {
				throw new ValidationException(
						"Cannot create goods receipt for PO in " + po.getStatus() + " status");
			}
		}

		// GR number is auto-generated when the caller leaves it blank:
		// GR-<year>-<6 digits>, same convention as the PO number form.
		String grNumber = grDto.getGrNumber();
		if (grNumber == null || grNumber.isBlank()) {
			grNumber = String.format("GR-%d-%06d", java.time.LocalDate.now().getYear(),
					System.currentTimeMillis() % 1000000);
		}
		if (goodsReceiptRepo.existsByGrNumber(grNumber)) {
			throw new ValidationException("GR number already exists");
		}

		GoodsReceipt gr = new GoodsReceipt();
		gr.setGrNumber(grNumber);
		gr.setPurchaseOrder(po);
		gr.setShipment(shipment);
		gr.setAsnId(grDto.getAsnId());
		gr.setSupplier(supplier);
		gr.setStatus(GoodsReceiptStatus.DRAFT);
		gr.setReceivedDate(grDto.getReceivedDate());
		gr.setDeliveryNoteNumber(grDto.getDeliveryNoteNumber());
		gr.setCarrier(grDto.getCarrier());
		gr.setTrackingNumber(grDto.getTrackingNumber());
		gr.setNotes(grDto.getNotes());

		if (shipmentSource) {
			if (gr.getAsnId() == null) {
				asnRepo.findByShipmentId(shipment.getShipmentId()).stream().findFirst()
						.ifPresent(a -> gr.setAsnId(a.getAsnId()));
			}
			if (grDto.getLineItems() == null || grDto.getLineItems().isEmpty()) {
				throw new ValidationException("At least one line item is required");
			}
			for (GoodsReceiptLineItemDto lineDto : grDto.getLineItems()) {
				GoodsReceiptLineItem lineItem = toLineItemEntity(lineDto);
				Material material = lineDto.getMaterialId() != null
						? materialRepo.findById(lineDto.getMaterialId()).orElse(null) : null;
				if (material == null) {
					material = shipment.getMaterial();
				}
				if (material == null) {
					throw new ValidationException("Material is required for a core shipment receipt");
				}
				lineItem.setMaterial(material);
				lineItem.setDescription(lineDto.getDescription() != null
						? lineDto.getDescription() : material.getName());
				if (lineItem.getUnitOfMeasure() == null) {
					lineItem.setUnitOfMeasure(material.getUnit());
				}
				gr.addLineItem(lineItem);
			}
		} else if (grDto.getLineItems() != null && !grDto.getLineItems().isEmpty()) {
			for (GoodsReceiptLineItemDto lineDto : grDto.getLineItems()) {
				PurchaseOrderLineItem poLineItem = poLineItemRepo.findById(lineDto.getPoLineItemId())
						.orElseThrow(() -> new ResourceNotFoundException("PurchaseOrderLineItem",
								"poLineItemId", lineDto.getPoLineItemId()));
				if (!poLineItem.getPurchaseOrder().getPurchaseOrderId()
						.equals(po.getPurchaseOrderId())) {
					throw new ValidationException(
							"PO line item does not belong to the specified purchase order");
				}
				GoodsReceiptLineItem lineItem = toLineItemEntity(lineDto);
				lineItem.setPoLineItem(poLineItem);
				gr.addLineItem(lineItem);
			}
		}

		GoodsReceipt savedGr = goodsReceiptRepo.save(gr);
		return new ResponseEntity<>(toDto(savedGr), HttpStatus.CREATED);
	}

	private static boolean isCoreReference(String referenceType) {
		return "CONSIGNMENT_FILL".equals(referenceType)
				|| "VMI_REPLENISHMENT".equals(referenceType);
	}

	@Override
	public ResponseEntity<?> getGoodsReceiptById(Long id) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		GoodsReceipt gr = goodsReceiptRepo.findVisibleToRetailerById(id, orgId);
		if (gr == null) {
			throw new ResourceNotFoundException("GoodsReceipt", "goodsReceiptId", id);
		}
		return new ResponseEntity<>(toDto(gr), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getAllGoodsReceipts(String status, Long purchaseOrderId, Long supplierId,
			Long shipmentId, Pageable pageable) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		// Default to newest first when the caller passes no explicit sort
		// (an explicit ?sort= is respected).
		Pageable sorted = pageable.getSort().isUnsorted()
				? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
						Sort.by(Sort.Direction.DESC, "createdAt"))
				: pageable;
		Page<GoodsReceipt> grs;

		if (status != null && !status.isBlank()) {
			try {
				GoodsReceiptStatus grStatus = GoodsReceiptStatus.valueOf(status.toUpperCase());
				if (purchaseOrderId != null) {
					grs = goodsReceiptRepo.findByPurchaseOrderAndStatus(purchaseOrderId, grStatus, sorted);
				} else if (supplierId != null) {
					grs = goodsReceiptRepo.findBySupplierSupplierId(supplierId, sorted);
				} else if (shipmentId != null) {
					grs = goodsReceiptRepo.findByShipmentShipmentId(shipmentId, sorted);
				} else {
					grs = goodsReceiptRepo.findVisibleToRetailer(orgId, grStatus, sorted);
				}
			} catch (IllegalArgumentException e) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid status: " + status);
			}
		} else if (purchaseOrderId != null) {
			grs = goodsReceiptRepo.findByPurchaseOrderPurchaseOrderId(purchaseOrderId, sorted);
		} else if (supplierId != null) {
			grs = goodsReceiptRepo.findBySupplierSupplierId(supplierId, sorted);
		} else if (shipmentId != null) {
			grs = goodsReceiptRepo.findByShipmentShipmentId(shipmentId, sorted);
		} else {
			grs = goodsReceiptRepo.findVisibleToRetailer(orgId, null, sorted);
		}

		Page<GoodsReceiptDto> grDtos = grs.map(GoodsReceiptServiceImpl::toDto);
		return new ResponseEntity<>(grDtos, HttpStatus.OK);
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<?> getSourceContext(Long shipmentId) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Shipment shipment = shipmentRepo.findById(shipmentId)
				.orElseThrow(() -> new ResourceNotFoundException("Shipment", "shipmentId", shipmentId));
		if (shipment.getRetailerOrg() == null
				|| !shipment.getRetailerOrg().getAccountId().equals(orgId)) {
			throw new ResourceNotFoundException("Shipment", "shipmentId", shipmentId);
		}
		if (!isCoreReference(shipment.getReferenceType())) {
			throw new ValidationException(
					"Only consignment/VMI core shipments can be received without a purchase order");
		}
		Supplier supplier = null;
		if (shipment.getSupplierOrg() != null) {
			Long supplierOrgId = shipment.getSupplierOrg().getAccountId();
			supplier = supplierRepo.findFirstBySupplierOrgAccountId(supplierOrgId)
					.orElseGet(() -> supplierRepo
							.findByAccountAccountId(supplierOrgId, Pageable.unpaged())
							.getContent().stream().findFirst().orElse(null));
		}
		GoodsReceipt existing = goodsReceiptRepo
				.findByShipmentShipmentId(shipmentId, Pageable.unpaged()).getContent().stream()
				.filter(g -> g.getStatus() != GoodsReceiptStatus.CANCELLED)
				.findFirst().orElse(null);
		AdvanceShipmentNotice asn = asnRepo.findByShipmentId(shipmentId).stream()
				.findFirst().orElse(null);
		Material material = shipment.getMaterial();

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("shipmentId", shipment.getShipmentId());
		res.put("shipmentNumber", shipment.getShipmentNumber());
		res.put("shipmentStatus", shipment.getStatus() != null ? shipment.getStatus().name() : null);
		res.put("referenceType", shipment.getReferenceType());
		res.put("referenceId", shipment.getReferenceId());
		res.put("supplierId", supplier != null ? supplier.getSupplierId() : null);
		res.put("supplierName", supplier != null ? supplier.getBusinessName() : null);
		res.put("supplierOrgId", shipment.getSupplierOrg() != null
				? shipment.getSupplierOrg().getAccountId() : null);
		res.put("retailerOrgId", shipment.getRetailerOrg() != null
				? shipment.getRetailerOrg().getAccountId() : null);
		res.put("materialId", material != null ? material.getMaterialId() : null);
		res.put("materialName", material != null ? material.getName() : null);
		res.put("materialCode", material != null ? material.getCode() : null);
		res.put("quantity", shipment.getShippedQuantity());
		res.put("unitOfMeasure", shipment.getQuantityUom() != null
				? shipment.getQuantityUom() : (material != null ? material.getUnit() : null));
		res.put("asnId", asn != null ? asn.getAsnId() : null);
		res.put("asnNumber", asn != null ? asn.getAsnNumber() : null);
		res.put("existingGoodsReceiptId", existing != null ? existing.getGoodsReceiptId() : null);
		res.put("existingGrNumber", existing != null ? existing.getGrNumber() : null);
		return ResponseEntity.ok(res);
	}

	@Override
	@Transactional
	public ResponseEntity<?> updateGoodsReceipt(Long id, GoodsReceiptDto grDto) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		GoodsReceipt gr = goodsReceiptRepo.findVisibleToRetailerById(id, orgId);
		if (gr == null) {
			throw new ResourceNotFoundException("GoodsReceipt", "goodsReceiptId", id);
		}

		// Only allow updates in DRAFT status
		if (gr.getStatus() != GoodsReceiptStatus.DRAFT) {
			throw new ValidationException("Can only update goods receipts in DRAFT status");
		}

		// Update fields
		gr.setDeliveryNoteNumber(grDto.getDeliveryNoteNumber());
		gr.setCarrier(grDto.getCarrier());
		gr.setTrackingNumber(grDto.getTrackingNumber());
		gr.setNotes(grDto.getNotes());
		gr.setReceivedDate(grDto.getReceivedDate());

		// Update line items - replace all
		gr.getLineItems().clear();
		if (grDto.getLineItems() != null && !grDto.getLineItems().isEmpty()) {
			for (GoodsReceiptLineItemDto lineDto : grDto.getLineItems()) {
				GoodsReceiptLineItem lineItem = toLineItemEntity(lineDto);
				if (gr.getShipment() != null) {
					Material material = lineDto.getMaterialId() != null
							? materialRepo.findById(lineDto.getMaterialId()).orElse(null) : null;
					if (material == null) {
						material = gr.getShipment().getMaterial();
					}
					if (material == null) {
						throw new ValidationException("Material is required for a core shipment receipt");
					}
					lineItem.setMaterial(material);
					lineItem.setDescription(lineDto.getDescription() != null
							? lineDto.getDescription() : material.getName());
					if (lineItem.getUnitOfMeasure() == null) {
						lineItem.setUnitOfMeasure(material.getUnit());
					}
				} else {
					PurchaseOrderLineItem poLineItem = poLineItemRepo.findById(lineDto.getPoLineItemId())
							.orElseThrow(() -> new ResourceNotFoundException("PurchaseOrderLineItem",
									"poLineItemId", lineDto.getPoLineItemId()));
					lineItem.setPoLineItem(poLineItem);
				}
				gr.addLineItem(lineItem);
			}
		}

		GoodsReceipt savedGr = goodsReceiptRepo.save(gr);
		return new ResponseEntity<>(toDto(savedGr), HttpStatus.OK);
	}

	@Override
	@Transactional
	public ResponseEntity<?> transitionStatus(Long id, GoodsReceiptStatus newStatus, Map<String, Object> params) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		GoodsReceipt gr = goodsReceiptRepo.findVisibleToRetailerById(id, orgId);
		if (gr == null) {
			throw new ResourceNotFoundException("GoodsReceipt", "goodsReceiptId", id);
		}

		GoodsReceiptStatus currentStatus = gr.getStatus();

		// Validate state transition
		validateTransition(currentStatus, newStatus, params);

		// Execute transition
		executeTransition(gr, currentStatus, newStatus, params);

		GoodsReceipt savedGr = goodsReceiptRepo.save(gr);
		return new ResponseEntity<>(toDto(savedGr), HttpStatus.OK);
	}

	/**
	 * ModelMapper cannot map GoodsReceiptLineItemDto → GoodsReceiptLineItem:
	 * both *Id fields (grLineItemId/poLineItemId) match the nested
	 * destination goodsReceipt.goodsReceiptId. Map scalars explicitly; the
	 * identity and both relations are resolved by the caller.
	 */
	private GoodsReceiptLineItem toLineItemEntity(GoodsReceiptLineItemDto lineDto) {
		GoodsReceiptLineItem lineItem = new GoodsReceiptLineItem();
		lineItem.setLineNumber(lineDto.getLineNumber());
		lineItem.setQuantityReceived(lineDto.getQuantityReceived());
		lineItem.setQuantityAccepted(lineDto.getQuantityAccepted());
		lineItem.setQuantityRejected(lineDto.getQuantityRejected());
		lineItem.setUnitOfMeasure(lineDto.getUnitOfMeasure());
		lineItem.setNotes(lineDto.getNotes());
		return lineItem;
	}

	/**
	 * ModelMapper cannot map GoodsReceipt → GoodsReceiptDto either: the
	 * destination purchaseOrderId matches both nested source paths
	 * purchaseOrder.purchaseOrderId and purchaseOrder.parentPoId (same
	 * *Id ambiguity family as the line items). Map explicitly.
	 */
	public static GoodsReceiptDto toDto(GoodsReceipt gr) {
		GoodsReceiptDto dto = new GoodsReceiptDto();
		dto.setGoodsReceiptId(gr.getGoodsReceiptId());
		dto.setGrNumber(gr.getGrNumber());
		dto.setPurchaseOrderId(
				gr.getPurchaseOrder() != null ? gr.getPurchaseOrder().getPurchaseOrderId() : null);
		dto.setShipmentId(
				gr.getShipment() != null ? gr.getShipment().getShipmentId() : null);
		dto.setAsnId(gr.getAsnId());
		dto.setSupplierId(gr.getSupplier() != null ? gr.getSupplier().getSupplierId() : null);
		dto.setStatus(gr.getStatus());
		dto.setReceivedDate(gr.getReceivedDate());
		dto.setDeliveryNoteNumber(gr.getDeliveryNoteNumber());
		dto.setCarrier(gr.getCarrier());
		dto.setTrackingNumber(gr.getTrackingNumber());
		dto.setNotes(gr.getNotes());
		if (gr.getLineItems() != null) {
			java.util.List<GoodsReceiptLineItemDto> lines = new java.util.ArrayList<>(gr.getLineItems().size());
			for (GoodsReceiptLineItem line : gr.getLineItems()) {
				GoodsReceiptLineItemDto lineDto = new GoodsReceiptLineItemDto();
				lineDto.setGrLineItemId(line.getGrLineItemId());
				lineDto.setPoLineItemId(
						line.getPoLineItem() != null ? line.getPoLineItem().getLineItemId() : null);
				lineDto.setMaterialId(
						line.getMaterial() != null ? line.getMaterial().getMaterialId() : null);
				lineDto.setDescription(line.getDescription());
				lineDto.setLineNumber(line.getLineNumber());
				lineDto.setQuantityReceived(line.getQuantityReceived());
				lineDto.setQuantityAccepted(line.getQuantityAccepted());
				lineDto.setQuantityRejected(line.getQuantityRejected());
				lineDto.setUnitOfMeasure(line.getUnitOfMeasure());
				lineDto.setNotes(line.getNotes());
				lines.add(lineDto);
			}
			dto.setLineItems(lines);
		}
		return dto;
	}

	private void validateTransition(GoodsReceiptStatus currentStatus, GoodsReceiptStatus newStatus,
			Map<String, Object> params) {
		switch (currentStatus) {
			case DRAFT:
				if (newStatus != GoodsReceiptStatus.RECEIVED && newStatus != GoodsReceiptStatus.CANCELLED) {
					throw new ValidationException("From DRAFT, can only transition to RECEIVED or CANCELLED");
				}
				if (newStatus == GoodsReceiptStatus.RECEIVED) {
					if (params == null || params.get("lineItems") == null
							|| ((List<?>) params.get("lineItems")).isEmpty()) {
						throw new ValidationException("Cannot receive goods without line items");
					}
				}
				break;
		case RECEIVED:
			if (newStatus != GoodsReceiptStatus.SENT_TO_SUPPLIER
					&& newStatus != GoodsReceiptStatus.RETURNED
					&& newStatus != GoodsReceiptStatus.CANCELLED) {
				throw new ValidationException(
						"From RECEIVED, can only transition to SENT_TO_SUPPLIER, RETURNED or CANCELLED");
			}
			break;
		case SENT_TO_SUPPLIER:
			if (newStatus != GoodsReceiptStatus.RETURNED && newStatus != GoodsReceiptStatus.CANCELLED) {
				throw new ValidationException(
						"From SENT_TO_SUPPLIER, can only transition to RETURNED or CANCELLED");
			}
			break;
			case RETURNED:
				if (newStatus != GoodsReceiptStatus.CANCELLED) {
					throw new ValidationException("From RETURNED, can only transition to CANCELLED");
				}
				break;
			case CANCELLED:
				throw new ValidationException("Cannot transition from CANCELLED - terminal state");
			default:
				throw new ValidationException("Invalid status transition from " + currentStatus + " to " + newStatus);
		}
	}

	private void executeTransition(GoodsReceipt gr, GoodsReceiptStatus currentStatus, GoodsReceiptStatus newStatus,
			Map<String, Object> params) {
		gr.setStatus(newStatus);

		switch (newStatus) {
		case RECEIVED:
			gr.setReceivedAt(Timestamp.valueOf(LocalDateTime.now()));
			if (gr.getShipment() != null) {
				receiveCoreShipment(gr);
			} else {
				// Block receiving unless production warehouse capacity is set
				Long buyerOrgId = gr.getPurchaseOrder() != null && gr.getPurchaseOrder().getBuyerOrg() != null
						? gr.getPurchaseOrder().getBuyerOrg().getAccountId() : null;
				if (buyerOrgId == null) buyerOrgId = OrganizationContextHolder.getCurrentOrganizationId();
				Warehouse prodWh = warehouseRepo.findByOrg(buyerOrgId, PageRequest.of(0, 10)).getContent().stream()
						.filter(w -> "PRODUCTION".equalsIgnoreCase(w.getWarehouseType())).findFirst().orElse(null);
				if (prodWh == null || prodWh.getCapacity() == null || prodWh.getCapacity() <= 0) {
					throw new ValidationException("Production warehouse capacity must be set before receiving materials");
				}
				// Receiving accepts whatever was counted unless the line was
				// explicitly part-accepted/rejected: without this default the
				// accepted totals stay null and the PO can never flip to
				// RECEIVED (which in turn gates invoicing).
				if (gr.getLineItems() != null) {
					for (GoodsReceiptLineItem line : gr.getLineItems()) {
						if (line.getQuantityAccepted() == null) {
							line.setQuantityAccepted(line.getQuantityReceived());
						}
						// Derive a retailer Material (and Stock) from the
						// received PO line's supplier catalog item. Materials are
						// not created manually by the retailer.
						try {
							receiveLineIntoInventory(gr, line);
						} catch (Exception e) {
							log.error("Failed to derive material/stock from GR line {}", line.getGrLineItemId(), e);
						}
					}
				}
				// Update PO status if all items received
				if (gr.getPurchaseOrder() != null) {
					updatePurchaseOrderStatus(gr.getPurchaseOrder());
				}
			}
			break;
		case SENT_TO_SUPPLIER:
			// Publishing to the supplier: the receipt becomes visible to
			// the supplier org for validation and invoicing.
			gr.setSharedWithSupplier(true);
			break;
			case RETURNED:
				gr.setReturnedAt(Timestamp.valueOf(LocalDateTime.now()));
				break;
			case CANCELLED:
				gr.setCancelledAt(Timestamp.valueOf(LocalDateTime.now()));
				break;
			default:
				break;
		}
	}

	/**
	 * Create or update a retailer Material from the received PO line's
	 * supplier catalog item, and add the accepted quantity to Stock.
	 * Materials are never created manually by the retailer; they are
	 * derived from goods received against a purchase order.
	 */
	private void receiveLineIntoInventory(GoodsReceipt gr, GoodsReceiptLineItem line) {
		PurchaseOrderLineItem poLine = line.getPoLineItem();
		if (poLine == null) return;

		Long buyerOrgId = gr.getPurchaseOrder() != null && gr.getPurchaseOrder().getBuyerOrg() != null
				? gr.getPurchaseOrder().getBuyerOrg().getAccountId() : null;
		if (buyerOrgId == null) {
			buyerOrgId = OrganizationContextHolder.getCurrentOrganizationId();
		}
		if (buyerOrgId == null) return;

		// Resolve the supplier catalog item (if the PO line references one).
		SupplierCatalog catalog = null;
		if (poLine.getCatalogId() != null) {
			Long supplierOrgId = gr.getSupplier() != null ? gr.getSupplier().getSupplierOrgAccountId() : null;
			if (supplierOrgId != null) {
				catalog = supplierCatalogRepo.findByCatalogIdAndSupplierOrgAccountId(poLine.getCatalogId(), supplierOrgId)
						.orElse(null);
			}
			if (catalog == null) {
				catalog = supplierCatalogRepo.findById(poLine.getCatalogId()).orElse(null);
			}
		}

		BigDecimal accepted = line.getQuantityAccepted() != null ? line.getQuantityAccepted()
				: (line.getQuantityReceived() != null ? line.getQuantityReceived() : BigDecimal.ZERO);
		if (accepted.compareTo(BigDecimal.ZERO) <= 0) return;

		Long catalogId = poLine.getCatalogId() != null ? poLine.getCatalogId()
				: (catalog != null ? catalog.getCatalogId() : null);

		// Find or create the Material for this buyer org + catalog item.
		Material material = null;
		if (catalogId != null) {
			material = materialRepo.findFirstByOrgAndCatalogId(buyerOrgId, catalogId).orElse(null);
		}
		if (material == null) {
			material = new Material();
			material.setOrg(buyerOrgId);
			material.setCatalogId(catalogId);
		}
		if (catalog != null) {
			material.setName(catalog.getName());
			material.setCode(catalog.getCode());
			material.setCategory(catalog.getCategory() != null ? catalog.getCategory() : catalog.getFamily());
			material.setVariant(catalog.getFamily());
			material.setUnit(catalog.getUnitOfMeasure());
			material.setCurrency(catalog.getCurrency() != null ? catalog.getCurrency() : "USD");
			material.setPricePerUnit(catalog.getBasePrice());
			material.setSupplierOrgId(catalog.getSupplierOrg() != null ? catalog.getSupplierOrg().getAccountId() : null);
		} else if (poLine.getDescription() != null) {
			material.setName(poLine.getDescription());
			material.setCode(poLine.getDescription());
		}
		if (poLine.getUnitOfMeasure() != null) {
			material.setUnit(poLine.getUnitOfMeasure());
		}
		if (poLine.getUnitPrice() != null) {
			material.setPricePerUnit(poLine.getUnitPrice());
		}
		if (material.getName() == null) {
			material.setName("Received item");
		}
		if (material.getCode() == null) {
			material.setCode("MAT-" + System.currentTimeMillis());
		}

		// Persist material first so stock rows can reference it.
		material = materialRepo.save(material);

		addStockForMaterial(buyerOrgId, material, accepted);
		log.info("Derived material {} (catalog {}) for org {} from GR line {}", material.getMaterialId(),
				material.getCatalogId(), buyerOrgId, line.getGrLineItemId());

		// Update PO line received quantity.
		Double prevReceived = poLine.getQuantityReceived() != null ? poLine.getQuantityReceived() : 0.0;
		poLine.setQuantityReceived(prevReceived + accepted.doubleValue());
		poLine.setMaterial(material);
		poLineItemRepo.save(poLine);

	}

	/**
	 * Add accepted stock to the retailer's reserve warehouses in fill order,
	 * respecting capacity. Shared by PO receipts (material derived from the
	 * supplier catalog) and core-shipment receipts (material carried on the
	 * shipment).
	 */
	private void addStockForMaterial(Long buyerOrgId, Material material, BigDecimal accepted) {
		if (buyerOrgId == null || material == null || accepted == null
				|| accepted.compareTo(BigDecimal.ZERO) <= 0) {
			return;
		}
		List<Warehouse> reserves = new ArrayList<>();
		try {
			reserves = warehouseRepo.findByOrg(buyerOrgId, Pageable.unpaged()).getContent().stream()
					.filter(w -> "RESERVE".equalsIgnoreCase(w.getWarehouseType()))
					.sorted(java.util.Comparator.comparingInt(
							w -> w.getFillOrder() != null ? w.getFillOrder() : Integer.MAX_VALUE))
					.collect(java.util.stream.Collectors.toList());
		} catch (Exception ignored) {
		}
		if (reserves.isEmpty()) {
			Warehouse wh = new Warehouse();
			wh.setOrg(buyerOrgId);
			wh.setCode("RESERVE-1");
			wh.setLocation("Reserve 1");
			wh.setWarehouseType("RESERVE");
			wh.setCapacityMultiplier(2.0);
			wh.setCapacity(0.0);
			wh.setStorageCapacity(0.0);
			wh.setFillOrder(1);
			wh = warehouseRepo.save(wh);
			reserves.add(wh);
		}
		Warehouse firstReserve = reserves.get(0);
		material.setWarehouse(firstReserve);

		double remaining = accepted.doubleValue();
		for (Warehouse w : reserves) {
			if (remaining <= 0) break;
			double cap = w.getCapacity() != null ? w.getCapacity()
					: (w.getStorageCapacity() != null ? w.getStorageCapacity() : 0.0);
			double used = 0.0;
			Stock existing = stockRepo.findByMaterialAndWarehouse(material, w).orElse(null);
			if (existing != null && existing.getQuantityOnHand() != null) used = existing.getQuantityOnHand();
			double availCap = cap > 0 ? Math.max(0, cap - used) : Double.MAX_VALUE;
			double take = (cap > 0) ? Math.min(availCap, remaining) : remaining;
			if (take <= 0) continue;
			Stock st = existing;
			if (st == null) {
				st = new Stock();
				st.setMaterial(material);
				st.setWarehouse(w);
				st.setQuantityOnHand(0.0);
				st.setQuantityReserved(0.0);
				st.setQuantityAvailable(0.0);
				if (material.getPricePerUnit() != null) {
					st.setAverageCost(material.getPricePerUnit());
					st.setLastCost(material.getPricePerUnit());
					st.setStandardCost(material.getPricePerUnit());
				}
			}
			st.setQuantityOnHand((st.getQuantityOnHand() != null ? st.getQuantityOnHand() : 0.0) + take);
			st.recalculateAvailable();
			stockRepo.save(st);
			remaining -= take;
		}
		if (remaining > 0 && !reserves.isEmpty()) {
			Warehouse last = reserves.get(reserves.size() - 1);
			Stock st = stockRepo.findByMaterialAndWarehouse(material, last).orElse(null);
			if (st == null) {
				st = new Stock();
				st.setMaterial(material);
				st.setWarehouse(last);
				st.setQuantityOnHand(0.0);
				st.setQuantityReserved(0.0);
				st.setQuantityAvailable(0.0);
			}
			st.setQuantityOnHand((st.getQuantityOnHand() != null ? st.getQuantityOnHand() : 0.0) + remaining);
			st.recalculateAvailable();
			stockRepo.save(st);
		}
	}

	/**
	 * Receive a core shipment: add accepted quantities to stock, mark the
	 * shipment received, and acknowledge its ASN. No PO status updates.
	 */
	private void receiveCoreShipment(GoodsReceipt gr) {
		Shipment shipment = gr.getShipment();
		Long buyerOrgId = shipment.getRetailerOrg() != null
				? shipment.getRetailerOrg().getAccountId() : null;
		if (buyerOrgId == null) {
			buyerOrgId = OrganizationContextHolder.getCurrentOrganizationId();
		}
		if (gr.getLineItems() != null) {
			for (GoodsReceiptLineItem line : gr.getLineItems()) {
				if (line.getQuantityAccepted() == null) {
					line.setQuantityAccepted(line.getQuantityReceived());
				}
				Material material = line.getMaterial() != null
						? line.getMaterial() : shipment.getMaterial();
				BigDecimal accepted = line.getQuantityAccepted() != null
						? line.getQuantityAccepted() : BigDecimal.ZERO;
				addStockForMaterial(buyerOrgId, material, accepted);
			}
		}
		shipmentRepo.save(shipment);
		if (gr.getAsnId() != null) {
			asnRepo.findById(gr.getAsnId()).ifPresent(a -> {
				a.setStatus("RECEIVED");
				asnRepo.save(a);
			});
		} else {
			asnRepo.findByShipmentId(shipment.getShipmentId()).stream().findFirst()
					.ifPresent(a -> {
						a.setStatus("RECEIVED");
						asnRepo.save(a);
					});
		}
	}

	private void updatePurchaseOrderStatus(PurchaseOrder po) {
		// Check if all PO line items have been fully received
		boolean allReceived = po.getLineItems().stream().allMatch(lineItem -> {
			BigDecimal totalReceived = grLineItemRepo.findByPoLineItemLineItemId(lineItem.getLineItemId()).stream()
					.map(li -> li.getQuantityAccepted() != null ? li.getQuantityAccepted() : BigDecimal.ZERO)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			return lineItem.getQuantityOrdered() != null
					&& totalReceived.compareTo(BigDecimal.valueOf(lineItem.getQuantityOrdered())) >= 0;
		});

		if (allReceived) {
			po.setStatus(PurchaseOrderStatus.RECEIVED);
		} else {
			po.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
		}
		purchaseOrderRepo.save(po);
	}
}