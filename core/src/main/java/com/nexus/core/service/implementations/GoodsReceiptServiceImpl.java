package com.nexus.core.service.implementations;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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
import com.nexus.core.model.entities.PurchaseOrder;
import com.nexus.core.model.entities.PurchaseOrderLineItem;
import com.nexus.core.model.enums.PurchaseOrderStatus;
import com.nexus.core.model.entities.Supplier;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.exception.ValidationException;
import com.nexus.core.payload.GoodsReceiptDto;
import com.nexus.core.payload.GoodsReceiptLineItemDto;
import com.nexus.core.repository.GoodsReceiptLineItemRepo;
import com.nexus.core.repository.GoodsReceiptRepo;
import com.nexus.core.repository.PurchaseOrderLineItemRepo;
import com.nexus.core.repository.PurchaseOrderRepo;
import com.nexus.core.repository.SupplierRepository;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.interfaces.GoodsReceiptService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GoodsReceiptServiceImpl implements GoodsReceiptService {

	private final GoodsReceiptRepo goodsReceiptRepo;
	private final GoodsReceiptLineItemRepo grLineItemRepo;
	private final PurchaseOrderRepo purchaseOrderRepo;
	private final PurchaseOrderLineItemRepo poLineItemRepo;
	private final SupplierRepository supplierRepo;

	@Override
	@Transactional
	public ResponseEntity<?> createGoodsReceipt(GoodsReceiptDto grDto) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		// Validate purchase order
		PurchaseOrder po = purchaseOrderRepo.findByPurchaseOrderIdAndBuyerOrgAccountId(grDto.getPurchaseOrderId(), orgId)
				.orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "purchaseOrderId",
						grDto.getPurchaseOrderId()));

		// Validate supplier
		Supplier supplier = supplierRepo.findBySupplierIdAndAccountAccountId(grDto.getSupplierId(), orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier", "supplierId", grDto.getSupplierId()));

		// Check if supplier matches PO supplier
		if (!supplier.getSupplierId().equals(po.getSupplier().getSupplierId())) {
			throw new ValidationException("Supplier does not match the purchase order supplier");
		}

		// Check PO status - must be ACKNOWLEDGED or higher to receive goods
		if (po.getStatus() == PurchaseOrderStatus.DRAFT || po.getStatus() == PurchaseOrderStatus.PENDING_APPROVAL
				|| po.getStatus() == PurchaseOrderStatus.REJECTED || po.getStatus() == PurchaseOrderStatus.CANCELLED) {
			throw new ValidationException("Cannot create goods receipt for PO in " + po.getStatus() + " status");
		}

		// GR number is auto-generated when the caller leaves it blank:
		// GR-<year>-<6 digits>, same convention as the PO number form.
		String grNumber = grDto.getGrNumber();
		if (grNumber == null || grNumber.isBlank()) {
			grNumber = String.format("GR-%d-%06d", java.time.LocalDate.now().getYear(),
					System.currentTimeMillis() % 1000000);
		}

		// Check for duplicate GR number
		if (goodsReceiptRepo.existsByGrNumber(grNumber)) {
			throw new ValidationException("GR number already exists");
		}

		// Create goods receipt
		GoodsReceipt gr = new GoodsReceipt();
		gr.setGrNumber(grNumber);
		gr.setPurchaseOrder(po);
		gr.setSupplier(supplier);
		gr.setStatus(GoodsReceiptStatus.DRAFT);
		gr.setReceivedDate(grDto.getReceivedDate());
		gr.setDeliveryNoteNumber(grDto.getDeliveryNoteNumber());
		gr.setCarrier(grDto.getCarrier());
		gr.setTrackingNumber(grDto.getTrackingNumber());
		gr.setNotes(grDto.getNotes());

		// Add line items
		if (grDto.getLineItems() != null && !grDto.getLineItems().isEmpty()) {
			for (GoodsReceiptLineItemDto lineDto : grDto.getLineItems()) {
				PurchaseOrderLineItem poLineItem = poLineItemRepo.findById(lineDto.getPoLineItemId())
						.orElseThrow(() -> new ResourceNotFoundException("PurchaseOrderLineItem", "poLineItemId",
								lineDto.getPoLineItemId()));

				// Validate PO line item belongs to the PO
				if (!poLineItem.getPurchaseOrder().getPurchaseOrderId().equals(po.getPurchaseOrderId())) {
					throw new ValidationException("PO line item does not belong to the specified purchase order");
				}

				GoodsReceiptLineItem lineItem = toLineItemEntity(lineDto);
				lineItem.setPoLineItem(poLineItem);
				gr.addLineItem(lineItem);
			}
		}

		GoodsReceipt savedGr = goodsReceiptRepo.save(gr);
		return new ResponseEntity<>(toDto(savedGr), HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<?> getGoodsReceiptById(Long id) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		GoodsReceipt gr = goodsReceiptRepo.findByGoodsReceiptIdAndPurchaseOrderBuyerOrgId(id, orgId);
		if (gr == null) {
			throw new ResourceNotFoundException("GoodsReceipt", "goodsReceiptId", id);
		}
		return new ResponseEntity<>(toDto(gr), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getAllGoodsReceipts(String status, Long purchaseOrderId, Long supplierId,
			Pageable pageable) {
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
				} else {
					grs = goodsReceiptRepo.findByStatus(grStatus, sorted);
				}
			} catch (IllegalArgumentException e) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid status: " + status);
			}
		} else if (purchaseOrderId != null) {
			grs = goodsReceiptRepo.findByPurchaseOrderPurchaseOrderId(purchaseOrderId, sorted);
		} else if (supplierId != null) {
			grs = goodsReceiptRepo.findBySupplierSupplierId(supplierId, sorted);
		} else {
			grs = goodsReceiptRepo.findByPurchaseOrderBuyerOrgId(orgId, sorted);
		}

		Page<GoodsReceiptDto> grDtos = grs.map(GoodsReceiptServiceImpl::toDto);
		return new ResponseEntity<>(grDtos, HttpStatus.OK);
	}

	@Override
	@Transactional
	public ResponseEntity<?> updateGoodsReceipt(Long id, GoodsReceiptDto grDto) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		GoodsReceipt gr = goodsReceiptRepo.findByGoodsReceiptIdAndPurchaseOrderBuyerOrgId(id, orgId);
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
				PurchaseOrderLineItem poLineItem = poLineItemRepo.findById(lineDto.getPoLineItemId())
						.orElseThrow(() -> new ResourceNotFoundException("PurchaseOrderLineItem", "poLineItemId",
								lineDto.getPoLineItemId()));

				GoodsReceiptLineItem lineItem = toLineItemEntity(lineDto);
				lineItem.setPoLineItem(poLineItem);
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
		GoodsReceipt gr = goodsReceiptRepo.findByGoodsReceiptIdAndPurchaseOrderBuyerOrgId(id, orgId);
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
			// Receiving accepts whatever was counted unless the line was
			// explicitly part-accepted/rejected: without this default the
			// accepted totals stay null and the PO can never flip to
			// RECEIVED (which in turn gates invoicing).
			if (gr.getLineItems() != null) {
				for (GoodsReceiptLineItem line : gr.getLineItems()) {
					if (line.getQuantityAccepted() == null) {
						line.setQuantityAccepted(line.getQuantityReceived());
					}
				}
			}
			// Update PO status if all items received
			updatePurchaseOrderStatus(gr.getPurchaseOrder());
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