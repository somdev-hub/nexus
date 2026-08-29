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
import com.nexus.core.entities.Invoice;
import com.nexus.core.entities.InvoiceLineItem;
import com.nexus.core.entities.InvoiceStatus;
import com.nexus.core.entities.PurchaseOrder;
import com.nexus.core.entities.PurchaseOrderLineItem;
import com.nexus.core.entities.PurchaseOrderStatus;
import com.nexus.core.entities.Supplier;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.exception.ValidationException;
import com.nexus.core.payload.InvoiceDto;
import com.nexus.core.payload.InvoiceLineItemDto;
import com.nexus.core.repository.AccountRepo;
import com.nexus.core.repository.InvoiceLineItemRepo;
import com.nexus.core.repository.InvoiceRepo;
import com.nexus.core.repository.PurchaseOrderLineItemRepo;
import com.nexus.core.repository.PurchaseOrderRepo;
import com.nexus.core.repository.SupplierRepository;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.InvoiceService;
import com.nexus.core.service.ThreeWayMatchingService;
import com.nexus.core.service.ThreeWayMatchingService.MatchingResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

	private final InvoiceRepo invoiceRepo;
	private final InvoiceLineItemRepo invoiceLineItemRepo;
	private final PurchaseOrderRepo purchaseOrderRepo;
	private final PurchaseOrderLineItemRepo poLineItemRepo;
	private final SupplierRepository supplierRepo;
	private final AccountRepo accountRepo;
	private final ModelMapper modelMapper;
	private final ThreeWayMatchingService threeWayMatchingService;

	@Override
	@Transactional
	public ResponseEntity<?> createInvoice(InvoiceDto invoiceDto) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		// Validate purchase order
		PurchaseOrder po = purchaseOrderRepo.findByPurchaseOrderIdAndBuyerOrgId(invoiceDto.getPurchaseOrderId(), orgId)
				.orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "purchaseOrderId",
						invoiceDto.getPurchaseOrderId()));

		// Validate supplier
		Supplier supplier = supplierRepo.findBySupplierIdAndAccountAccountId(invoiceDto.getSupplierId(), orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier", "supplierId", invoiceDto.getSupplierId()));

		// Check if supplier matches PO supplier
		if (!supplier.getSupplierId().equals(po.getSupplier().getSupplierId())) {
			throw new ValidationException("Supplier does not match the purchase order supplier");
		}

		// Check PO status - must be RECEIVED or INVOICED to create invoice
		if (po.getStatus() != PurchaseOrderStatus.RECEIVED && po.getStatus() != PurchaseOrderStatus.INVOICED
				&& po.getStatus() != PurchaseOrderStatus.PAID) {
			throw new ValidationException(
					"Cannot create invoice for PO in " + po.getStatus() + " status. PO must be RECEIVED or higher.");
		}

		// Check for duplicate invoice number
		if (invoiceRepo.existsByInvoiceNumber(invoiceDto.getInvoiceNumber())) {
			throw new ValidationException("Invoice number already exists");
		}

		// Create invoice
		Invoice invoice = new Invoice();
		invoice.setInvoiceNumber(invoiceDto.getInvoiceNumber());
		invoice.setPurchaseOrder(po);
		invoice.setSupplier(supplier);
		invoice.setStatus(InvoiceStatus.DRAFT);
		invoice.setInvoiceDate(invoiceDto.getInvoiceDate());
		invoice.setDueDate(invoiceDto.getDueDate());
		invoice.setCurrency(invoiceDto.getCurrency());
		invoice.setPaymentTerms(invoiceDto.getPaymentTerms());
		invoice.setSupplierInvoiceNumber(invoiceDto.getSupplierInvoiceNumber());
		invoice.setNotes(invoiceDto.getNotes());

		// Add line items
		if (invoiceDto.getLineItems() != null && !invoiceDto.getLineItems().isEmpty()) {
			for (InvoiceLineItemDto lineDto : invoiceDto.getLineItems()) {
				PurchaseOrderLineItem poLineItem = poLineItemRepo.findById(lineDto.getPoLineItemId())
						.orElseThrow(() -> new ResourceNotFoundException("PurchaseOrderLineItem", "poLineItemId",
								lineDto.getPoLineItemId()));

				// Validate PO line item belongs to the PO
				if (!poLineItem.getPurchaseOrder().getPurchaseOrderId().equals(po.getPurchaseOrderId())) {
					throw new ValidationException("PO line item does not belong to the specified purchase order");
				}

				InvoiceLineItem lineItem = modelMapper.map(lineDto, InvoiceLineItem.class);
				lineItem.setPoLineItem(poLineItem);

				// Link to GR line item if provided
				if (lineDto.getGrLineItemId() != null) {
					// This would need a GR line item repo - for now we'll skip
				}

				invoice.addLineItem(lineItem);
			}
		}

		Invoice savedInvoice = invoiceRepo.save(invoice);
		return new ResponseEntity<>(modelMapper.map(savedInvoice, InvoiceDto.class), HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<?> getInvoiceById(Long id) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Invoice invoice = invoiceRepo.findByInvoiceIdAndPurchaseOrderBuyerOrgId(id, orgId);
		if (invoice == null) {
			throw new ResourceNotFoundException("Invoice", "invoiceId", id);
		}
		return new ResponseEntity<>(modelMapper.map(invoice, InvoiceDto.class), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getAllInvoices(String status, Long purchaseOrderId, Long supplierId, Pageable pageable) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Page<Invoice> invoices;

		if (status != null && !status.isBlank()) {
			try {
				InvoiceStatus invStatus = InvoiceStatus.valueOf(status.toUpperCase());
				if (purchaseOrderId != null) {
					invoices = invoiceRepo.findByPurchaseOrderAndStatus(purchaseOrderId, invStatus, pageable);
				} else if (supplierId != null) {
					invoices = invoiceRepo.findBySupplierSupplierId(supplierId, pageable);
				} else {
					invoices = invoiceRepo.findByStatus(invStatus, pageable);
				}
			} catch (IllegalArgumentException e) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid status: " + status);
			}
		} else if (purchaseOrderId != null) {
			invoices = invoiceRepo.findByPurchaseOrderPurchaseOrderId(purchaseOrderId, pageable);
		} else if (supplierId != null) {
			invoices = invoiceRepo.findBySupplierSupplierId(supplierId, pageable);
		} else {
			invoices = invoiceRepo.findByPurchaseOrderBuyerOrgId(orgId, pageable);
		}

		Page<InvoiceDto> invoiceDtos = invoices.map(inv -> modelMapper.map(inv, InvoiceDto.class));
		return new ResponseEntity<>(invoiceDtos, HttpStatus.OK);
	}

	@Override
	@Transactional
	public ResponseEntity<?> updateInvoice(Long id, InvoiceDto invoiceDto) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Invoice invoice = invoiceRepo.findByInvoiceIdAndPurchaseOrderBuyerOrgId(id, orgId);
		if (invoice == null) {
			throw new ResourceNotFoundException("Invoice", "invoiceId", id);
		}

		// Only allow updates in DRAFT status
		if (invoice.getStatus() != InvoiceStatus.DRAFT) {
			throw new ValidationException("Can only update invoices in DRAFT status");
		}

		// Update fields
		invoice.setDueDate(invoiceDto.getDueDate());
		invoice.setCurrency(invoiceDto.getCurrency());
		invoice.setPaymentTerms(invoiceDto.getPaymentTerms());
		invoice.setSupplierInvoiceNumber(invoiceDto.getSupplierInvoiceNumber());
		invoice.setNotes(invoiceDto.getNotes());
		invoice.setInvoiceDate(invoiceDto.getInvoiceDate());

		// Update line items - replace all
		invoice.getLineItems().clear();
		if (invoiceDto.getLineItems() != null && !invoiceDto.getLineItems().isEmpty()) {
			for (InvoiceLineItemDto lineDto : invoiceDto.getLineItems()) {
				PurchaseOrderLineItem poLineItem = poLineItemRepo.findById(lineDto.getPoLineItemId())
						.orElseThrow(() -> new ResourceNotFoundException("PurchaseOrderLineItem", "poLineItemId",
								lineDto.getPoLineItemId()));

				InvoiceLineItem lineItem = modelMapper.map(lineDto, InvoiceLineItem.class);
				lineItem.setPoLineItem(poLineItem);
				invoice.addLineItem(lineItem);
			}
		}

		Invoice savedInvoice = invoiceRepo.save(invoice);
		return new ResponseEntity<>(modelMapper.map(savedInvoice, InvoiceDto.class), HttpStatus.OK);
	}

	@Override
	@Transactional
	public ResponseEntity<?> transitionStatus(Long id, InvoiceStatus newStatus, Map<String, Object> params) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Invoice invoice = invoiceRepo.findByInvoiceIdAndPurchaseOrderBuyerOrgId(id, orgId);
		if (invoice == null) {
			throw new ResourceNotFoundException("Invoice", "invoiceId", id);
		}

		InvoiceStatus currentStatus = invoice.getStatus();

		// Validate state transition
		validateTransition(invoice, currentStatus, newStatus, params);

		// Execute transition
		executeTransition(invoice, currentStatus, newStatus, params);

		Invoice savedInvoice = invoiceRepo.save(invoice);
		return new ResponseEntity<>(modelMapper.map(savedInvoice, InvoiceDto.class), HttpStatus.OK);
	}

	private void validateTransition(Invoice invoice, InvoiceStatus currentStatus, InvoiceStatus newStatus,
			Map<String, Object> params) {
		switch (currentStatus) {
			case DRAFT:
				if (newStatus != InvoiceStatus.PENDING_APPROVAL && newStatus != InvoiceStatus.CANCELLED) {
					throw new ValidationException("From DRAFT, can only transition to PENDING_APPROVAL or CANCELLED");
				}
				if (newStatus == InvoiceStatus.PENDING_APPROVAL) {
					if (params == null || params.get("lineItems") == null
							|| ((List<?>) params.get("lineItems")).isEmpty()) {
						throw new ValidationException("Cannot submit invoice without line items");
					}
				}
				break;
			case PENDING_APPROVAL:
				if (newStatus != InvoiceStatus.APPROVED && newStatus != InvoiceStatus.REJECTED
						&& newStatus != InvoiceStatus.CANCELLED) {
					throw new ValidationException(
							"From PENDING_APPROVAL, can only transition to APPROVED, REJECTED, or CANCELLED");
				}
				if (newStatus == InvoiceStatus.APPROVED) {
					// Three-way matching validation before approval
					MatchingResult matchResult = threeWayMatchingService
							.performThreeWayMatch(invoice.getPurchaseOrder());
					if (!matchResult.isMatched()) {
						throw new ValidationException("Three-way matching failed: " + matchResult.getMessage());
					}
				}
				if (newStatus == InvoiceStatus.REJECTED && (params == null || params.get("rejectionReason") == null)) {
					throw new ValidationException("Rejection reason is required");
				}
				break;
			case APPROVED:
				if (newStatus != InvoiceStatus.PAID && newStatus != InvoiceStatus.CANCELLED) {
					throw new ValidationException("From APPROVED, can only transition to PAID or CANCELLED");
				}
				break;
			case PAID:
				if (newStatus != InvoiceStatus.CANCELLED) {
					throw new ValidationException("From PAID, can only transition to CANCELLED");
				}
				break;
			case REJECTED:
				if (newStatus != InvoiceStatus.DRAFT && newStatus != InvoiceStatus.CANCELLED) {
					throw new ValidationException("From REJECTED, can only transition to DRAFT or CANCELLED");
				}
				break;
			case CANCELLED:
				throw new ValidationException("Cannot transition from CANCELLED - terminal state");
			default:
				throw new ValidationException("Invalid status transition from " + currentStatus + " to " + newStatus);
		}
	}

	private void executeTransition(Invoice invoice, InvoiceStatus currentStatus, InvoiceStatus newStatus,
			Map<String, Object> params) {
		invoice.setStatus(newStatus);

		switch (newStatus) {
			case PENDING_APPROVAL:
				invoice.setSubmittedAt(Timestamp.valueOf(LocalDateTime.now()));
				break;
			case APPROVED:
				invoice.setApprovedAt(Timestamp.valueOf(LocalDateTime.now()));
				invoice.setApprovedBy((String) params.get("approvedBy"));
				// Update PO status to INVOICED if not already
				PurchaseOrder po = invoice.getPurchaseOrder();
				if (po.getStatus() == PurchaseOrderStatus.RECEIVED) {
					po.setStatus(PurchaseOrderStatus.INVOICED);
					purchaseOrderRepo.save(po);
				}
				break;
			case REJECTED:
				invoice.setRejectionReason((String) params.get("rejectionReason"));
				break;
			case PAID:
				invoice.setPaidAt(Timestamp.valueOf(LocalDateTime.now()));
				break;
			case CANCELLED:
				invoice.setCancelledAt(Timestamp.valueOf(LocalDateTime.now()));
				break;
			case DRAFT:
				// Reset from REJECTED
				invoice.setSubmittedAt(null);
				invoice.setRejectionReason(null);
				break;
		}
	}
}