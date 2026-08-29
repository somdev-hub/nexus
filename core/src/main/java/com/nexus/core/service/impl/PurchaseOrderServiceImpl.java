package com.nexus.core.service.impl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.core.entities.Account;
import com.nexus.core.entities.ApprovalLevel;
import com.nexus.core.entities.Invoice;
import com.nexus.core.entities.InvoiceStatus;
import com.nexus.core.entities.Material;
import com.nexus.core.entities.Partnership;
import com.nexus.core.entities.Product;
import com.nexus.core.entities.PurchaseOrder;
import com.nexus.core.entities.PurchaseOrderLineItem;
import com.nexus.core.entities.PurchaseOrderStatus;
import com.nexus.core.entities.Supplier;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.exception.ValidationException;
import com.nexus.core.payload.PurchaseOrderDto;
import com.nexus.core.payload.PurchaseOrderLineItemDto;
import com.nexus.core.repository.AccountRepo;
import com.nexus.core.repository.InvoiceRepo;
import com.nexus.core.repository.MaterialRepo;
import com.nexus.core.repository.PartnershipRepo;
import com.nexus.core.repository.ProductRepo;
import com.nexus.core.repository.PurchaseOrderLineItemRepo;
import com.nexus.core.repository.PurchaseOrderRepo;
import com.nexus.core.repository.SupplierRepository;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.PurchaseOrderService;
import com.nexus.core.service.ThreeWayMatchingService;
import com.nexus.core.service.ThreeWayMatchingService.MatchingResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

	private final PurchaseOrderRepo purchaseOrderRepo;
	private final PurchaseOrderLineItemRepo lineItemRepo;
	private final AccountRepo accountRepo;
	private final SupplierRepository supplierRepo;
	private final PartnershipRepo partnershipRepo;
	private final MaterialRepo materialRepo;
	private final ProductRepo productRepo;
	private final InvoiceRepo invoiceRepo;
	private final ModelMapper modelMapper;
	private final ThreeWayMatchingService threeWayMatchingService;

	@Value("${po.approval.threshold.auto:10000}")
	private Double autoApprovalThreshold;

	@Value("${po.approval.threshold.manager:50000}")
	private Double managerApprovalThreshold;

	@Value("${po.approval.threshold.director:100000}")
	private Double directorApprovalThreshold;

	@Value("${po.matching.tolerance:0.01}")
	private BigDecimal matchingTolerance;

	@Override
	@Transactional
	public ResponseEntity<?> createPurchaseOrder(PurchaseOrderDto poDto) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		// Validate buyer organization
		Account buyerOrg = accountRepo.findByAccountId(orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", orgId));

		// Validate supplier
		Supplier supplier = supplierRepo
				.findBySupplierIdAndAccountAccountId(poDto.getSupplierId(), orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier", "supplierId", poDto.getSupplierId()));

		// Check if supplier is active
		if (supplier.getStatus() != com.nexus.core.entities.SupplierStatus.ACTIVE) {
			throw new ValidationException("Cannot create PO for inactive supplier");
		}

		// Validate partnership if provided
		Partnership partnership = null;
		if (poDto.getPartnershipId() != null) {
			partnership = partnershipRepo
					.findByPartnershipIdAndPrimaryOrgAccountId(poDto.getPartnershipId(), orgId)
					.orElseThrow(() -> new ResourceNotFoundException("Partnership", "partnershipId",
							poDto.getPartnershipId()));

			// Check if partnership is active
			if (partnership.getStatus() != com.nexus.core.entities.PartnershipStatus.ACTIVE) {
				throw new ValidationException("Cannot create PO for inactive partnership");
			}
		}

		// Check for duplicate PO number
		if (purchaseOrderRepo.existsByPoNumberAndBuyerOrgId(poDto.getPoNumber(), orgId)) {
			throw new ValidationException("PO number already exists for this organization");
		}

		// Create purchase order
		PurchaseOrder po = new PurchaseOrder();
		po.setPoNumber(poDto.getPoNumber());
		po.setBuyerOrg(buyerOrg);
		po.setSupplier(supplier);
		po.setPartnership(partnership);
		po.setStatus(PurchaseOrderStatus.DRAFT);
		po.setCurrency(poDto.getCurrency());
		po.setPaymentTerms(poDto.getPaymentTerms());
		po.setIncoterms(poDto.getIncoterms());
		po.setRequestedDeliveryDate(poDto.getRequestedDeliveryDate());
		po.setExpectedDeliveryDate(poDto.getExpectedDeliveryDate());
		po.setNotes(poDto.getNotes());
		po.setIsBlanketOrder(poDto.getIsBlanketOrder());
		po.setBlanketStartDate(poDto.getBlanketStartDate());
		po.setBlanketEndDate(poDto.getBlanketEndDate());
		po.setReleaseSchedule(poDto.getReleaseSchedule());

		// Add line items
		if (poDto.getLineItems() != null && !poDto.getLineItems().isEmpty()) {
			for (PurchaseOrderLineItemDto lineDto : poDto.getLineItems()) {
				PurchaseOrderLineItem lineItem = modelMapper.map(lineDto, PurchaseOrderLineItem.class);

				// Validate material
				if (lineDto.getMaterialId() != null) {
					Material material = materialRepo
							.findByMaterialIdAndOrg(lineDto.getMaterialId(), orgId)
							.orElseThrow(() -> new ResourceNotFoundException("Material", "materialId",
									lineDto.getMaterialId()));
					lineItem.setMaterial(material);
				}

				// Validate product
				if (lineDto.getProductId() != null) {
					Product product = productRepo.findByProductIdAndOrg(lineDto.getProductId(), orgId)
							.orElseThrow(() -> new ResourceNotFoundException("Product", "productId",
									lineDto.getProductId()));
					lineItem.setProduct(product);
				}

				// Calculate total price
				if (lineItem.getQuantityOrdered() != null && lineItem.getUnitPrice() != null) {
					lineItem.setTotalPrice(lineItem.getQuantityOrdered() * lineItem.getUnitPrice());
				}

				po.addLineItem(lineItem);
			}
		}

		PurchaseOrder savedPo = purchaseOrderRepo.save(po);
		return new ResponseEntity<>(modelMapper.map(savedPo, PurchaseOrderDto.class), HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<?> getPurchaseOrderById(Long id) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		PurchaseOrder po = purchaseOrderRepo.findByPurchaseOrderIdAndBuyerOrgId(id, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "purchaseOrderId", id));
		return new ResponseEntity<>(modelMapper.map(po, PurchaseOrderDto.class), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getAllPurchaseOrders(String status, Pageable pageable) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Page<PurchaseOrder> pos;
		if (status != null && !status.isBlank()) {
			try {
				PurchaseOrderStatus poStatus = PurchaseOrderStatus.valueOf(status.toUpperCase());
				pos = purchaseOrderRepo.findByOrgIdAndStatusIn(orgId, List.of(poStatus), pageable);
			} catch (IllegalArgumentException e) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid status: " + status);
			}
		} else {
			pos = purchaseOrderRepo.findByBuyerOrgId(orgId, pageable);
		}
		Page<PurchaseOrderDto> poDtos = pos.map(po -> modelMapper.map(po, PurchaseOrderDto.class));
		return new ResponseEntity<>(poDtos, HttpStatus.OK);
	}

	@Override
	@Transactional
	public ResponseEntity<?> updatePurchaseOrder(Long id, PurchaseOrderDto poDto) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		PurchaseOrder po = purchaseOrderRepo.findByPurchaseOrderIdAndBuyerOrgId(id, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "purchaseOrderId", id));

		// Only allow updates in DRAFT status
		if (po.getStatus() != PurchaseOrderStatus.DRAFT) {
			throw new ValidationException("Can only update purchase orders in DRAFT status");
		}

		// Update fields
		po.setPaymentTerms(poDto.getPaymentTerms());
		po.setIncoterms(poDto.getIncoterms());
		po.setRequestedDeliveryDate(poDto.getRequestedDeliveryDate());
		po.setExpectedDeliveryDate(poDto.getExpectedDeliveryDate());
		po.setNotes(poDto.getNotes());
		po.setIsBlanketOrder(poDto.getIsBlanketOrder());
		po.setBlanketStartDate(poDto.getBlanketStartDate());
		po.setBlanketEndDate(poDto.getBlanketEndDate());
		po.setReleaseSchedule(poDto.getReleaseSchedule());

		// Update line items - replace all
		po.getLineItems().clear();
		if (poDto.getLineItems() != null && !poDto.getLineItems().isEmpty()) {
			for (PurchaseOrderLineItemDto lineDto : poDto.getLineItems()) {
				PurchaseOrderLineItem lineItem = modelMapper.map(lineDto, PurchaseOrderLineItem.class);
				if (lineItem.getQuantityOrdered() != null && lineItem.getUnitPrice() != null) {
					lineItem.setTotalPrice(lineItem.getQuantityOrdered() * lineItem.getUnitPrice());
				}
				po.addLineItem(lineItem);
			}
		}

		PurchaseOrder savedPo = purchaseOrderRepo.save(po);
		return new ResponseEntity<>(modelMapper.map(savedPo, PurchaseOrderDto.class), HttpStatus.OK);
	}

	@Override
	@Transactional
	public ResponseEntity<?> transitionStatus(Long id, PurchaseOrderStatus newStatus, Map<String, Object> params) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		PurchaseOrder po = purchaseOrderRepo.findByPurchaseOrderIdAndBuyerOrgId(id, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "purchaseOrderId", id));

		PurchaseOrderStatus currentStatus = po.getStatus();

		// Validate state transition
		validateTransition(currentStatus, newStatus, params);

		// Execute transition
		executeTransition(po, currentStatus, newStatus, params);

		// If AUTO approval was triggered, the status would have been changed to
		// APPROVED in executeTransition
		// We need to handle the case where the requested transition was
		// PENDING_APPROVAL but it became APPROVED
		if (currentStatus == PurchaseOrderStatus.DRAFT && newStatus == PurchaseOrderStatus.PENDING_APPROVAL
				&& po.getStatus() == PurchaseOrderStatus.APPROVED) {
			// Auto-approval occurred, return the approved PO
			PurchaseOrder savedPo = purchaseOrderRepo.save(po);
			return new ResponseEntity<>(modelMapper.map(savedPo, PurchaseOrderDto.class), HttpStatus.OK);
		}

		PurchaseOrder savedPo = purchaseOrderRepo.save(po);
		return new ResponseEntity<>(modelMapper.map(savedPo, PurchaseOrderDto.class), HttpStatus.OK);
	}

	private void validateTransition(PurchaseOrderStatus currentStatus, PurchaseOrderStatus newStatus,
			Map<String, Object> params) {
		// Define valid transitions
		switch (currentStatus) {
			case DRAFT:
				if (newStatus != PurchaseOrderStatus.PENDING_APPROVAL && newStatus != PurchaseOrderStatus.CANCELLED) {
					throw new ValidationException("From DRAFT, can only transition to PENDING_APPROVAL or CANCELLED");
				}
				if (newStatus == PurchaseOrderStatus.PENDING_APPROVAL) {
					if (params == null || params.get("lineItems") == null
							|| ((List<?>) params.get("lineItems")).isEmpty()) {
						throw new ValidationException("Cannot submit PO without line items");
					}
				}
				break;
			case PENDING_APPROVAL:
				if (newStatus != PurchaseOrderStatus.APPROVED && newStatus != PurchaseOrderStatus.REJECTED
						&& newStatus != PurchaseOrderStatus.CANCELLED) {
					throw new ValidationException(
							"From PENDING_APPROVAL, can only transition to APPROVED, REJECTED, or CANCELLED");
				}
				if (newStatus == PurchaseOrderStatus.APPROVED && (params == null || params.get("approvedBy") == null)) {
					throw new ValidationException("ApprovedBy is required for approval");
				}
				if (newStatus == PurchaseOrderStatus.REJECTED
						&& (params == null || params.get("rejectionReason") == null)) {
					throw new ValidationException("Rejection reason is required");
				}
				break;
			case APPROVED:
				if (newStatus != PurchaseOrderStatus.SENT_TO_SUPPLIER && newStatus != PurchaseOrderStatus.CANCELLED) {
					throw new ValidationException(
							"From APPROVED, can only transition to SENT_TO_SUPPLIER or CANCELLED");
				}
				break;
			case SENT_TO_SUPPLIER:
				if (newStatus != PurchaseOrderStatus.ACKNOWLEDGED && newStatus != PurchaseOrderStatus.CANCELLED) {
					throw new ValidationException(
							"From SENT_TO_SUPPLIER, can only transition to ACKNOWLEDGED or CANCELLED");
				}
				break;
			case ACKNOWLEDGED:
				if (newStatus != PurchaseOrderStatus.PARTIALLY_RECEIVED && newStatus != PurchaseOrderStatus.RECEIVED
						&& newStatus != PurchaseOrderStatus.CANCELLED) {
					throw new ValidationException(
							"From ACKNOWLEDGED, can only transition to PARTIALLY_RECEIVED, RECEIVED, or CANCELLED");
				}
				break;
			case PARTIALLY_RECEIVED:
				if (newStatus != PurchaseOrderStatus.RECEIVED && newStatus != PurchaseOrderStatus.CANCELLED) {
					throw new ValidationException(
							"From PARTIALLY_RECEIVED, can only transition to RECEIVED or CANCELLED");
				}
				break;
			case RECEIVED:
				if (newStatus != PurchaseOrderStatus.INVOICED && newStatus != PurchaseOrderStatus.CANCELLED) {
					throw new ValidationException("From RECEIVED, can only transition to INVOICED or CANCELLED");
				}
				break;
			case INVOICED:
				if (newStatus != PurchaseOrderStatus.PAID && newStatus != PurchaseOrderStatus.CANCELLED) {
					throw new ValidationException("From INVOICED, can only transition to PAID or CANCELLED");
				}
				break;
			case PAID:
				if (newStatus != PurchaseOrderStatus.CLOSED) {
					throw new ValidationException("From PAID, can only transition to CLOSED");
				}
				break;
			case REJECTED:
				if (newStatus != PurchaseOrderStatus.DRAFT && newStatus != PurchaseOrderStatus.CANCELLED) {
					throw new ValidationException("From REJECTED, can only transition to DRAFT or CANCELLED");
				}
				break;
			case CANCELLED:
			case CLOSED:
				throw new ValidationException("Cannot transition from " + currentStatus + " - terminal state");
			default:
				throw new ValidationException("Invalid status transition from " + currentStatus + " to " + newStatus);
		}
	}

	private void executeTransition(PurchaseOrder po, PurchaseOrderStatus currentStatus, PurchaseOrderStatus newStatus,
			Map<String, Object> params) {
		po.setStatus(newStatus);

		switch (newStatus) {
			case PENDING_APPROVAL:
				// Determine approval level based on total amount
				ApprovalLevel level = determineApprovalLevel(po.getTotalAmount());
				po.setApprovalLevel(level);
				po.setRequiredApproverLevel(level.name());
				// For AUTO approval, immediately approve
				if (level == ApprovalLevel.AUTO) {
					po.setStatus(PurchaseOrderStatus.APPROVED);
					po.setApprovedAt(Timestamp.valueOf(LocalDateTime.now()));
					po.setApprovedBy("SYSTEM_AUTO_APPROVAL");
				}
				break;
			case APPROVED:
				// Validate approval authority for non-AUTO levels
				if (po.getApprovalLevel() != ApprovalLevel.AUTO) {
					String approvedBy = (String) params.get("approvedBy");
					if (!hasApprovalAuthority(approvedBy, po.getApprovalLevel())) {
						throw new ValidationException(
								"User " + approvedBy + " does not have sufficient authority to approve at "
										+ po.getApprovalLevel() + " level");
					}
					po.setCurrentApprover(approvedBy);
				}
				po.setApprovedAt(Timestamp.valueOf(LocalDateTime.now()));
				po.setApprovedBy((String) params.get("approvedBy"));
				break;
			case REJECTED:
				po.setRejectionReason((String) params.get("rejectionReason"));
				break;
			case SENT_TO_SUPPLIER:
				po.setSentToSupplierAt(Timestamp.valueOf(LocalDateTime.now()));
				break;
			case ACKNOWLEDGED:
				po.setAcknowledgedAt(Timestamp.valueOf(LocalDateTime.now()));
				break;
			case PARTIALLY_RECEIVED:
			case RECEIVED:
				// These would be handled by receiving logic
				break;
			case INVOICED:
				// Three-way matching validation before allowing INVOICED status
				MatchingResult matchResult = threeWayMatchingService.performThreeWayMatch(po);
				if (!matchResult.isMatched()) {
					throw new ValidationException("Three-way matching failed: " + matchResult.getMessage());
				}
				break;
			case PAID:
				// Validate invoice exists and is approved before payment
				List<Invoice> approvedInvoices = invoiceRepo.findByPurchaseOrderAndStatus(
						po.getPurchaseOrderId(), InvoiceStatus.APPROVED, null).getContent();
				if (approvedInvoices.isEmpty()) {
					throw new ValidationException("Cannot mark as PAID: No approved invoice found for this PO");
				}
				// Verify three-way match again before payment
				MatchingResult paymentMatchResult = threeWayMatchingService.performThreeWayMatch(po);
				if (!paymentMatchResult.isMatched()) {
					throw new ValidationException(
							"Three-way matching failed before payment: " + paymentMatchResult.getMessage());
				}
				break;
			case CANCELLED:
			case CLOSED:
				// Terminal states
				break;
			case DRAFT:
				// Reset to draft (from REJECTED)
				po.setApprovalLevel(null);
				po.setRequiredApproverLevel(null);
				po.setCurrentApprover(null);
				po.setApprovalDelegatedTo(null);
				break;
		}
	}

	/**
	 * Determine the approval level based on PO total amount.
	 * FR-RET-002: Value-based approval thresholds
	 * - Below $10,000 — automatic approval (AUTO)
	 * - $10,000 - $50,000 — manager approval (MANAGER)
	 * - Above $50,000 — director approval (DIRECTOR)
	 */
	private ApprovalLevel determineApprovalLevel(Double totalAmount) {
		if (totalAmount == null) {
			return ApprovalLevel.AUTO;
		}
		if (totalAmount < autoApprovalThreshold) {
			return ApprovalLevel.AUTO;
		} else if (totalAmount < managerApprovalThreshold) {
			return ApprovalLevel.MANAGER;
		} else {
			return ApprovalLevel.DIRECTOR;
		}
	}

	/**
	 * Validate that the user approving the PO has the required authority level.
	 * For AUTO approval level, no specific authority validation needed
	 * (auto-approved).
	 * For MANAGER level, user must have manager or higher authority.
	 * For DIRECTOR level, user must have director authority.
	 */
	private void validateApprovalAuthority(Map<String, Object> params) {
		String approvedBy = (String) params.get("approvedBy");
		if (approvedBy == null) {
			throw new ValidationException("ApprovedBy is required for approval");
		}

		// Get the PO to check its approval level
		// Note: This is called from validateTransition which doesn't have PO context
		// The actual validation will happen in executeTransition where we have the PO
		// This method can be extended to check against HR organizational hierarchy
		// For now, we just ensure approvedBy is provided
	}

	/**
	 * Check if a user has the required approval authority for a given level.
	 * This would integrate with HR organizational hierarchy in a full
	 * implementation.
	 */
	private boolean hasApprovalAuthority(String userId, ApprovalLevel requiredLevel) {
		// TODO: Integrate with HR service to check user's role/level in organizational
		// hierarchy
		// For now, return true to allow approval flow to proceed
		// In production, this would call HR service to verify user's position/level
		return true;
	}

	@Override
	@Transactional
	public ResponseEntity<?> createAmendment(Long parentPoId, PurchaseOrderDto amendmentDto) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		PurchaseOrder parentPo = purchaseOrderRepo.findByPurchaseOrderIdAndBuyerOrgId(parentPoId, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "purchaseOrderId", parentPoId));

		// Create new PO as amendment
		amendmentDto.setParentPoId(parentPoId);
		amendmentDto.setRevisionNumber(parentPo.getRevisionNumber() + 1);
		amendmentDto.setPoNumber(parentPo.getPoNumber() + "-A" + amendmentDto.getRevisionNumber());
		amendmentDto.setStatus(PurchaseOrderStatus.DRAFT);

		return createPurchaseOrder(amendmentDto);
	}

	@Override
	public ResponseEntity<?> getAmendmentsByParentPoId(Long parentPoId) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		List<PurchaseOrder> amendments = purchaseOrderRepo.findAmendmentsByParentPoId(orgId, parentPoId);
		List<PurchaseOrderDto> amendmentDtos = amendments.stream()
				.map(po -> modelMapper.map(po, PurchaseOrderDto.class))
				.collect(Collectors.toList());
		return new ResponseEntity<>(amendmentDtos, HttpStatus.OK);
	}
}