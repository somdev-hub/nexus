package com.nexus.core.service.implementations;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.exception.ValidationException;
import com.nexus.core.model.entities.Invoice;
import com.nexus.core.model.entities.InvoiceLineItem;
import com.nexus.core.model.entities.PurchaseOrder;
import com.nexus.core.model.entities.PurchaseOrderLineItem;
import com.nexus.core.model.entities.Supplier;
import com.nexus.core.model.enums.InvoiceStatus;
import com.nexus.core.model.enums.PurchaseOrderStatus;
import com.nexus.core.payload.InvoiceDto;
import com.nexus.core.payload.InvoiceLineItemDto;
import com.nexus.core.repository.InvoiceRepo;
import com.nexus.core.repository.PurchaseOrderLineItemRepo;
import com.nexus.core.repository.PurchaseOrderRepo;
import com.nexus.core.repository.SupplierRepository;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.interfaces.InvoiceService;
import com.nexus.core.service.interfaces.ThreeWayMatchingService;
import com.nexus.core.service.interfaces.ThreeWayMatchingService.MatchingResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

	private final InvoiceRepo invoiceRepo;
	private final PurchaseOrderRepo purchaseOrderRepo;
	private final PurchaseOrderLineItemRepo poLineItemRepo;
	private final SupplierRepository supplierRepo;
	private final ThreeWayMatchingService threeWayMatchingService;

	@Override
	@Transactional
	public ResponseEntity<?> createInvoice(InvoiceDto invoiceDto) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		// Validate purchase order
		PurchaseOrder po = purchaseOrderRepo.findByPurchaseOrderIdAndBuyerOrgAccountId(invoiceDto.getPurchaseOrderId(), orgId)
				.orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "purchaseOrderId",
						invoiceDto.getPurchaseOrderId()));

		// Validate supplier
		if (invoiceDto.getSupplierId() == null) {
			throw new ValidationException("Supplier is required");
		}
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

		// Invoice number is auto-generated when the caller leaves it blank:
		// INV-<year>-<6 digits>.
		String invoiceNumber = invoiceDto.getInvoiceNumber();
		if (invoiceNumber == null || invoiceNumber.isBlank()) {
			invoiceNumber = String.format("INV-%d-%06d", java.time.LocalDate.now().getYear(),
					System.currentTimeMillis() % 1000000);
		}

		// Check for duplicate invoice number
		if (invoiceRepo.existsByInvoiceNumber(invoiceNumber)) {
			throw new ValidationException("Invoice number already exists");
		}

		// Create invoice
		Invoice invoice = new Invoice();
		invoice.setInvoiceNumber(invoiceNumber);
		invoice.setPurchaseOrder(po);
		invoice.setSupplier(supplier);
		invoice.setStatus(InvoiceStatus.DRAFT);
		populateInvoice(invoice, po, invoiceDto);

		Invoice savedInvoice = invoiceRepo.save(invoice);
		return new ResponseEntity<>(toDto(savedInvoice), HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<?> getInvoiceById(Long id) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Invoice invoice = invoiceRepo.findByInvoiceIdAndPurchaseOrderBuyerOrgId(id, orgId);
		if (invoice == null) {
			throw new ResourceNotFoundException("Invoice", "invoiceId", id);
		}
		return new ResponseEntity<>(toDto(invoice), HttpStatus.OK);
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

		Page<InvoiceDto> invoiceDtos = invoices.map(InvoiceServiceImpl::toDto);
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

		// Update fields (header) + replace all line items. The linked
		// purchase order itself is immutable here — lines are validated
		// against the invoice's own PO.
		invoice.getLineItems().clear();
		populateInvoice(invoice, invoice.getPurchaseOrder(), invoiceDto);

		Invoice savedInvoice = invoiceRepo.save(invoice);
		return new ResponseEntity<>(toDto(savedInvoice), HttpStatus.OK);
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
		return new ResponseEntity<>(toDto(savedInvoice), HttpStatus.OK);
	}

	/**
	 * Shared header + line population for create/update (both retailer and
	 * supplier flows). Line PO ownership is always validated against the
	 * invoice's own purchase order.
	 */
	private void populateInvoice(Invoice invoice, PurchaseOrder po, InvoiceDto invoiceDto) {
		invoice.setInvoiceDate(invoiceDto.getInvoiceDate());
		invoice.setDueDate(invoiceDto.getDueDate());
		invoice.setCurrency(invoiceDto.getCurrency());
		invoice.setPaymentTerms(invoiceDto.getPaymentTerms());
		invoice.setSupplierInvoiceNumber(invoiceDto.getSupplierInvoiceNumber());
		invoice.setTaxAmount(invoiceDto.getTaxAmount());
		invoice.setOtherCharges(invoiceDto.getOtherCharges());
		invoice.setOtherChargesBreakdown(invoiceDto.getOtherChargesBreakdown());
		invoice.setNotes(invoiceDto.getNotes());
		addLineItems(invoice, po, invoiceDto);
	}

	private void addLineItems(Invoice invoice, PurchaseOrder po, InvoiceDto invoiceDto) {
		if (invoiceDto.getLineItems() == null || invoiceDto.getLineItems().isEmpty()) {
			return;
		}
		for (InvoiceLineItemDto lineDto : invoiceDto.getLineItems()) {
			PurchaseOrderLineItem poLineItem = poLineItemRepo.findById(lineDto.getPoLineItemId())
					.orElseThrow(() -> new ResourceNotFoundException("PurchaseOrderLineItem", "poLineItemId",
							lineDto.getPoLineItemId()));
			if (po == null || poLineItem.getPurchaseOrder() == null
					|| !poLineItem.getPurchaseOrder().getPurchaseOrderId().equals(po.getPurchaseOrderId())) {
				throw new ValidationException("PO line item does not belong to the specified purchase order");
			}
			InvoiceLineItem lineItem = toLineItemEntity(lineDto);
			lineItem.setPoLineItem(poLineItem);
			// Link to GR line item if provided
			if (lineDto.getGrLineItemId() != null) {
				// This would need a GR line item repo - for now we'll skip
			}
			invoice.addLineItem(lineItem);
		}
	}

	/**
	 * ModelMapper cannot map InvoiceLineItemDto → InvoiceLineItem: the
	 * destination invoice.invoiceId matches several source *Id hierarchies
	 * (invoiceLineItemId/poLineItemId/grLineItemId — same *Id ambiguity
	 * family as goods receipts). Map scalars explicitly; identity and
	 * relations are resolved by the caller. Line total derives from
	 * quantity × price when the caller leaves it empty.
	 */
	private static InvoiceLineItem toLineItemEntity(InvoiceLineItemDto lineDto) {
		InvoiceLineItem lineItem = new InvoiceLineItem();
		lineItem.setLineNumber(lineDto.getLineNumber());
		lineItem.setQuantityInvoiced(lineDto.getQuantityInvoiced());
		lineItem.setUnitPrice(lineDto.getUnitPrice());
		java.math.BigDecimal total = lineDto.getLineTotal();
		if (total == null && lineDto.getQuantityInvoiced() != null && lineDto.getUnitPrice() != null) {
			total = lineDto.getQuantityInvoiced().multiply(lineDto.getUnitPrice());
		}
		lineItem.setLineTotal(total);
		lineItem.setUnitOfMeasure(lineDto.getUnitOfMeasure());
		lineItem.setNotes(lineDto.getNotes());
		return lineItem;
	}

	/**
	 * ModelMapper cannot map Invoice → InvoiceDto either: the destination
	 * purchaseOrderId matches both nested source paths
	 * purchaseOrder.purchaseOrderId and purchaseOrder.parentPoId. Map
	 * explicitly.
	 */
	private static InvoiceDto toDto(Invoice inv) {
		InvoiceDto dto = new InvoiceDto();
		dto.setInvoiceId(inv.getInvoiceId());
		dto.setInvoiceNumber(inv.getInvoiceNumber());
		dto.setPurchaseOrderId(
				inv.getPurchaseOrder() != null ? inv.getPurchaseOrder().getPurchaseOrderId() : null);
		dto.setPoNumber(
				inv.getPurchaseOrder() != null ? inv.getPurchaseOrder().getPoNumber() : null);
		dto.setSupplierId(inv.getSupplier() != null ? inv.getSupplier().getSupplierId() : null);
		dto.setStatus(inv.getStatus());
		dto.setInvoiceDate(inv.getInvoiceDate());
		dto.setDueDate(inv.getDueDate());
		dto.setTotalAmount(inv.getTotalAmount());
		dto.setTaxAmount(inv.getTaxAmount());
		dto.setOtherCharges(inv.getOtherCharges());
		dto.setOtherChargesBreakdown(inv.getOtherChargesBreakdown());
		dto.setCurrency(inv.getCurrency());
		dto.setPaymentTerms(inv.getPaymentTerms());
		dto.setSupplierInvoiceNumber(inv.getSupplierInvoiceNumber());
		dto.setNotes(inv.getNotes());
		if (inv.getLineItems() != null) {
			java.util.List<InvoiceLineItemDto> lines = new java.util.ArrayList<>(inv.getLineItems().size());
			for (InvoiceLineItem line : inv.getLineItems()) {
				InvoiceLineItemDto lineDto = new InvoiceLineItemDto();
				lineDto.setInvoiceLineItemId(line.getInvoiceLineItemId());
				lineDto.setPoLineItemId(
						line.getPoLineItem() != null ? line.getPoLineItem().getLineItemId() : null);
				lineDto.setGrLineItemId(
						line.getGrLineItem() != null ? line.getGrLineItem().getGrLineItemId() : null);
				lineDto.setLineNumber(line.getLineNumber());
				lineDto.setQuantityInvoiced(line.getQuantityInvoiced());
				lineDto.setUnitPrice(line.getUnitPrice());
				lineDto.setLineTotal(line.getLineTotal());
				lineDto.setUnitOfMeasure(line.getUnitOfMeasure());
				lineDto.setNotes(line.getNotes());
				lines.add(lineDto);
			}
			dto.setLineItems(lines);
		}
		return dto;
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
					// Three-way matching validation before approval — the
					// invoice under review is included in the match.
					MatchingResult matchResult = threeWayMatchingService
							.performThreeWayMatch(invoice.getPurchaseOrder(), invoice.getInvoiceId());
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

	// ============================================
	// Supplier-raised invoicing
	// ============================================

	private PurchaseOrder findSupplierOrder(Long purchaseOrderId, Long orgId) {
		return purchaseOrderRepo.findSupplierVisibleOrderById(purchaseOrderId, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "purchaseOrderId",
						purchaseOrderId));
	}

	private static void requireReceivable(PurchaseOrder po) {
		if (po.getStatus() != PurchaseOrderStatus.RECEIVED && po.getStatus() != PurchaseOrderStatus.INVOICED
				&& po.getStatus() != PurchaseOrderStatus.PAID) {
			throw new ValidationException(
					"Cannot create invoice for PO in " + po.getStatus() + " status. PO must be RECEIVED or higher.");
		}
	}

	@Override
	@Transactional
	public ResponseEntity<?> createSupplierInvoice(InvoiceDto invoiceDto) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		PurchaseOrder po = findSupplierOrder(invoiceDto.getPurchaseOrderId(), orgId);
		requireReceivable(po);
		Supplier supplier = po.getSupplier();
		if (supplier == null) {
			throw new ValidationException("Purchase order has no supplier");
		}
		if (invoiceDto.getSupplierInvoiceNumber() == null || invoiceDto.getSupplierInvoiceNumber().isBlank()) {
			throw new ValidationException("Supplier invoice number is required");
		}

		String invoiceNumber = invoiceDto.getInvoiceNumber();
		if (invoiceNumber == null || invoiceNumber.isBlank()) {
			invoiceNumber = String.format("INV-%d-%06d", java.time.LocalDate.now().getYear(),
					System.currentTimeMillis() % 1000000);
		}
		if (invoiceRepo.existsByInvoiceNumber(invoiceNumber)) {
			throw new ValidationException("Invoice number already exists");
		}

		Invoice invoice = new Invoice();
		invoice.setInvoiceNumber(invoiceNumber);
		invoice.setPurchaseOrder(po);
		invoice.setSupplier(supplier);
		invoice.setStatus(InvoiceStatus.DRAFT);
		populateInvoice(invoice, po, invoiceDto);

		Invoice savedInvoice = invoiceRepo.save(invoice);
		return new ResponseEntity<>(toDto(savedInvoice), HttpStatus.CREATED);
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<?> getSupplierInvoiceById(Long id) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Invoice invoice = invoiceRepo.findByInvoiceIdAndSupplierOrg(id, orgId);
		if (invoice == null) {
			throw new ResourceNotFoundException("Invoice", "invoiceId", id);
		}
		return new ResponseEntity<>(toDto(invoice), HttpStatus.OK);
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<?> getSupplierInvoices(String status, Long purchaseOrderId, Pageable pageable) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		InvoiceStatus st = null;
		if (status != null && !status.isBlank()) {
			try {
				st = InvoiceStatus.valueOf(status.toUpperCase());
			} catch (IllegalArgumentException e) {
				return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(List.of(), pageable, 0));
			}
		}
		Page<Invoice> invoices = invoiceRepo.findSupplierVisibleInvoices(orgId, null, st, pageable);
		if (purchaseOrderId != null) {
			java.util.List<InvoiceDto> filtered = invoices.getContent().stream()
					.filter(inv -> inv.getPurchaseOrder() != null
							&& purchaseOrderId.equals(inv.getPurchaseOrder().getPurchaseOrderId()))
					.map(InvoiceServiceImpl::toDto).toList();
			return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size()));
		}
		Page<InvoiceDto> invoiceDtos = invoices.map(InvoiceServiceImpl::toDto);
		return new ResponseEntity<>(invoiceDtos, HttpStatus.OK);
	}

	@Override
	@Transactional
	public ResponseEntity<?> updateSupplierInvoice(Long id, InvoiceDto invoiceDto) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Invoice invoice = invoiceRepo.findByInvoiceIdAndSupplierOrg(id, orgId);
		if (invoice == null) {
			throw new ResourceNotFoundException("Invoice", "invoiceId", id);
		}
		if (invoice.getStatus() != InvoiceStatus.DRAFT) {
			throw new ValidationException("Can only update invoices in DRAFT status");
		}
		invoice.getLineItems().clear();
		populateInvoice(invoice, invoice.getPurchaseOrder(), invoiceDto);
		Invoice savedInvoice = invoiceRepo.save(invoice);
		return new ResponseEntity<>(toDto(savedInvoice), HttpStatus.OK);
	}

	@Override
	@Transactional
	public ResponseEntity<?> transitionSupplierInvoice(Long id, InvoiceStatus newStatus,
			Map<String, Object> params) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Invoice invoice = invoiceRepo.findByInvoiceIdAndSupplierOrg(id, orgId);
		if (invoice == null) {
			throw new ResourceNotFoundException("Invoice", "invoiceId", id);
		}
		// Suppliers move their own invoices along the submit path only:
		// DRAFT → PENDING_APPROVAL/CANCELLED, REJECTED → DRAFT/CANCELLED,
		// PENDING_APPROVAL → CANCELLED (withdraw). Approval and payment
		// stay on the retailer side.
		InvoiceStatus current = invoice.getStatus();
		boolean allowed = (current == InvoiceStatus.DRAFT
				&& (newStatus == InvoiceStatus.PENDING_APPROVAL || newStatus == InvoiceStatus.CANCELLED))
				|| (current == InvoiceStatus.REJECTED
						&& (newStatus == InvoiceStatus.DRAFT || newStatus == InvoiceStatus.CANCELLED))
				|| (current == InvoiceStatus.PENDING_APPROVAL && newStatus == InvoiceStatus.CANCELLED);
		if (!allowed) {
			throw new ValidationException(
					"Supplier cannot transition invoice from " + current + " to " + newStatus);
		}
		validateTransition(invoice, current, newStatus, params);
		executeTransition(invoice, current, newStatus, params);
		Invoice savedInvoice = invoiceRepo.save(invoice);
		return new ResponseEntity<>(toDto(savedInvoice), HttpStatus.OK);
	}
}