package com.nexus.core.service.impl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.core.entities.Account;
import com.nexus.core.entities.GoodsReceipt;
import com.nexus.core.entities.GoodsReceiptLineItem;
import com.nexus.core.entities.GoodsReceiptStatus;
import com.nexus.core.entities.PurchaseOrder;
import com.nexus.core.entities.PurchaseOrderLineItem;
import com.nexus.core.entities.PurchaseOrderStatus;
import com.nexus.core.entities.Supplier;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.exception.ValidationException;
import com.nexus.core.payload.GoodsReceiptDto;
import com.nexus.core.payload.GoodsReceiptLineItemDto;
import com.nexus.core.repository.AccountRepo;
import com.nexus.core.repository.GoodsReceiptLineItemRepo;
import com.nexus.core.repository.GoodsReceiptRepo;
import com.nexus.core.repository.PurchaseOrderLineItemRepo;
import com.nexus.core.repository.PurchaseOrderRepo;
import com.nexus.core.repository.SupplierRepository;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.GoodsReceiptService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GoodsReceiptServiceImpl implements GoodsReceiptService {

	private final GoodsReceiptRepo goodsReceiptRepo;
	private final GoodsReceiptLineItemRepo grLineItemRepo;
	private final PurchaseOrderRepo purchaseOrderRepo;
	private final PurchaseOrderLineItemRepo poLineItemRepo;
	private final SupplierRepository supplierRepo;
	private final AccountRepo accountRepo;
	private final ModelMapper modelMapper;

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

		// Check for duplicate GR number
		if (goodsReceiptRepo.existsByGrNumber(grDto.getGrNumber())) {
			throw new ValidationException("GR number already exists");
		}

		// Create goods receipt
		GoodsReceipt gr = new GoodsReceipt();
		gr.setGrNumber(grDto.getGrNumber());
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

				GoodsReceiptLineItem lineItem = modelMapper.map(lineDto, GoodsReceiptLineItem.class);
				lineItem.setPoLineItem(poLineItem);
				gr.addLineItem(lineItem);
			}
		}

		GoodsReceipt savedGr = goodsReceiptRepo.save(gr);
		return new ResponseEntity<>(modelMapper.map(savedGr, GoodsReceiptDto.class), HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<?> getGoodsReceiptById(Long id) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		GoodsReceipt gr = goodsReceiptRepo.findByGoodsReceiptIdAndPurchaseOrderBuyerOrgId(id, orgId);
		if (gr == null) {
			throw new ResourceNotFoundException("GoodsReceipt", "goodsReceiptId", id);
		}
		return new ResponseEntity<>(modelMapper.map(gr, GoodsReceiptDto.class), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getAllGoodsReceipts(String status, Long purchaseOrderId, Long supplierId,
			Pageable pageable) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Page<GoodsReceipt> grs;

		if (status != null && !status.isBlank()) {
			try {
				GoodsReceiptStatus grStatus = GoodsReceiptStatus.valueOf(status.toUpperCase());
				if (purchaseOrderId != null) {
					grs = goodsReceiptRepo.findByPurchaseOrderAndStatus(purchaseOrderId, grStatus, pageable);
				} else if (supplierId != null) {
					grs = goodsReceiptRepo.findBySupplierSupplierId(supplierId, pageable);
				} else {
					grs = goodsReceiptRepo.findByStatus(grStatus, pageable);
				}
			} catch (IllegalArgumentException e) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid status: " + status);
			}
		} else if (purchaseOrderId != null) {
			grs = goodsReceiptRepo.findByPurchaseOrderPurchaseOrderId(purchaseOrderId, pageable);
		} else if (supplierId != null) {
			grs = goodsReceiptRepo.findBySupplierSupplierId(supplierId, pageable);
		} else {
			grs = goodsReceiptRepo.findByPurchaseOrderBuyerOrgId(orgId, pageable);
		}

		Page<GoodsReceiptDto> grDtos = grs.map(gr -> modelMapper.map(gr, GoodsReceiptDto.class));
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

				GoodsReceiptLineItem lineItem = modelMapper.map(lineDto, GoodsReceiptLineItem.class);
				lineItem.setPoLineItem(poLineItem);
				gr.addLineItem(lineItem);
			}
		}

		GoodsReceipt savedGr = goodsReceiptRepo.save(gr);
		return new ResponseEntity<>(modelMapper.map(savedGr, GoodsReceiptDto.class), HttpStatus.OK);
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
		return new ResponseEntity<>(modelMapper.map(savedGr, GoodsReceiptDto.class), HttpStatus.OK);
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
				if (newStatus != GoodsReceiptStatus.RETURNED && newStatus != GoodsReceiptStatus.CANCELLED) {
					throw new ValidationException("From RECEIVED, can only transition to RETURNED or CANCELLED");
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
				// Update PO status if all items received
				updatePurchaseOrderStatus(gr.getPurchaseOrder());
				break;
			case RETURNED:
				gr.setReturnedAt(Timestamp.valueOf(LocalDateTime.now()));
				break;
			case CANCELLED:
				gr.setCancelledAt(Timestamp.valueOf(LocalDateTime.now()));
				break;
		}
	}

	private void updatePurchaseOrderStatus(PurchaseOrder po) {
		// Check if all PO line items have been fully received
		boolean allReceived = po.getLineItems().stream().allMatch(lineItem -> {
			BigDecimal totalReceived = grLineItemRepo.findByPoLineItemLineItemId(lineItem.getLineItemId()).stream()
					.map(GoodsReceiptLineItem::getQuantityAccepted)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			return totalReceived.compareTo(BigDecimal.valueOf(lineItem.getQuantityOrdered())) >= 0;
		});

		if (allReceived) {
			po.setStatus(PurchaseOrderStatus.RECEIVED);
		} else {
			po.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
		}
		purchaseOrderRepo.save(po);
	}
}