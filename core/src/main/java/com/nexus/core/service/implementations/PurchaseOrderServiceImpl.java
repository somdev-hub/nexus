package com.nexus.core.service.implementations;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.exception.ValidationException;
import com.nexus.core.model.entities.Account;
import com.nexus.core.model.entities.Invoice;
import com.nexus.core.model.entities.Material;
import com.nexus.core.model.entities.Partnership;
import com.nexus.core.model.entities.Product;
import com.nexus.core.model.entities.PurchaseOrder;
import com.nexus.core.model.entities.PurchaseOrderLineItem;
import com.nexus.core.model.entities.Supplier;
import com.nexus.core.model.entities.SupplierQuotation;
import com.nexus.core.model.enums.ApprovalLevel;
import com.nexus.core.model.enums.InvoiceStatus;
import com.nexus.core.model.enums.PurchaseOrderStatus;
import com.nexus.core.payload.PurchaseOrderDto;
import com.nexus.core.payload.PurchaseOrderLineItemDto;
import com.nexus.core.repository.AccountRepo;
import com.nexus.core.repository.InvoiceRepo;
import com.nexus.core.repository.MaterialRepo;
import com.nexus.core.repository.PartnershipRepo;
import com.nexus.core.repository.ProductRepo;
import com.nexus.core.repository.PurchaseOrderRepo;
import com.nexus.core.repository.SupplierQuotationRepo;
import com.nexus.core.repository.SupplierRepository;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.interfaces.PurchaseOrderService;
import com.nexus.core.service.interfaces.ThreeWayMatchingService;
import com.nexus.core.service.interfaces.ThreeWayMatchingService.MatchingResult;
import com.nexus.core.utils.RestService;
import com.nexus.core.utils.WebConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

	private final PurchaseOrderRepo purchaseOrderRepo;
	private final AccountRepo accountRepo;
	private final SupplierRepository supplierRepo;
	private final SupplierQuotationRepo quotationRepo;
	private final PartnershipRepo partnershipRepo;
	private final MaterialRepo materialRepo;
	private final ProductRepo productRepo;
	private final InvoiceRepo invoiceRepo;
	private final ModelMapper modelMapper;
	private final ThreeWayMatchingService threeWayMatchingService;
	private final RestService restService;
	private final WebConstants webConstants;

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
		if (supplier.getStatus() != com.nexus.core.model.enums.SupplierStatus.ACTIVE) {
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
			if (partnership.getStatus() != com.nexus.core.model.enums.PartnershipStatus.ACTIVE) {
				throw new ValidationException("Cannot create PO for inactive partnership");
			}
		}

		// Check for duplicate PO number
		if (purchaseOrderRepo.existsByPoNumberAndBuyerOrgAccountId(poDto.getPoNumber(), orgId)) {
			throw new ValidationException("PO number already exists for this organization");
		}

		// Validate source quotation when converting from one (retailer flow):
		// only the buyer org may convert, only ACCEPTED quotations, only once.
		SupplierQuotation sourceQuotation = null;
		if (poDto.getSourceQuotationId() != null) {
			sourceQuotation = quotationRepo.findById(poDto.getSourceQuotationId())
					.orElseThrow(() -> new ResourceNotFoundException("SupplierQuotation", "quotationId",
							poDto.getSourceQuotationId()));
			if (sourceQuotation.getBuyerOrg() == null
					|| !sourceQuotation.getBuyerOrg().getAccountId().equals(orgId)) {
				throw new ValidationException("Only the buyer organization can convert quotation "
						+ poDto.getSourceQuotationId() + " to an order");
			}
			if (sourceQuotation.getStatus() != SupplierQuotation.QuotationStatus.ACCEPTED) {
				throw new ValidationException("Only ACCEPTED quotations can be converted to order. Current: "
						+ sourceQuotation.getStatus());
			}
			if (sourceQuotation.getConvertedToPoId() != null) {
				throw new ValidationException(
						"Quotation already converted to PO: " + sourceQuotation.getConvertedToPoId());
			}
		}

		// Create purchase order
		PurchaseOrder po = new PurchaseOrder();
		po.setPoNumber(poDto.getPoNumber());
		po.setBuyerOrg(buyerOrg);
		po.setSupplier(supplier);
		po.setPartnership(partnership);
		po.setSupplierOrg(resolveSupplierOrg(buyerOrg, supplier, partnership));
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
		po.setSourceQuotation(sourceQuotation);
		po.setShippingAddress(poDto.getShippingAddress());
		po.setBillingAddress(poDto.getBillingAddress());

		// TEMP-ADDRESS-DEFAULTS-START: fill blank shipping/billing addresses
		// from the buyer (creator) org's HR addresses (default-flagged, else
		// the single address if only one exists). TEMPORARY migration logic
		// — delete this call together with the TEMP-ADDRESS-DEFAULTS helpers
		// below once every PO is created with explicit addresses.
		// NOTE: always the PO's buyer org, never the viewing caller's org —
		// a supplier viewing the PO must not stamp its own addresses onto it.
		applyOrgAddressDefaults(po, buyerOrgIdOf(po, orgId));
		// TEMP-ADDRESS-DEFAULTS-END

		// Add line items
		if (poDto.getLineItems() != null && !poDto.getLineItems().isEmpty()) {
			for (PurchaseOrderLineItemDto lineDto : poDto.getLineItems()) {
				PurchaseOrderLineItem lineItem = toLineItemEntity(lineDto);

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
		if (sourceQuotation != null) {
			sourceQuotation.setStatus(SupplierQuotation.QuotationStatus.CONVERTED);
			sourceQuotation.setConvertedToPoId(savedPo.getPurchaseOrderId());
			quotationRepo.save(sourceQuotation);
		}
		return new ResponseEntity<>(toDto(savedPo), HttpStatus.CREATED);
	}

	/**
	 * Resolve the supplier counterparty Account for a new PO.
	 * (a) partnership attached: the partnership org that is not the buyer;
	 * (b) else the supplier's supplierOrgAccountId (missing Account tolerated as null);
	 * (c) else null (legacy fallback).
	 */
	private Account resolveSupplierOrg(Account buyerOrg, Supplier supplier, Partnership partnership) {
		if (partnership != null) {
			if (partnership.getPrimaryOrg() != null && partnership.getSecondaryOrg() != null) {
				if (partnership.getPrimaryOrg().getAccountId().equals(buyerOrg.getAccountId())) {
					return partnership.getSecondaryOrg();
				}
				return partnership.getPrimaryOrg();
			}
			if (partnership.getSecondaryOrg() != null) {
				return partnership.getSecondaryOrg();
			}
			if (partnership.getPrimaryOrg() != null
					&& !partnership.getPrimaryOrg().getAccountId().equals(buyerOrg.getAccountId())) {
				return partnership.getPrimaryOrg();
			}
		}
		if (supplier.getSupplierOrgAccountId() != null) {
			return accountRepo.findByAccountId(supplier.getSupplierOrgAccountId()).orElse(null);
		}
		return null;
	}

	@Override
	public ResponseEntity<?> getPurchaseOrderById(Long id) {		Long orgId = OrganizationContextHolder.requireOrganizationId();
		PurchaseOrder po = purchaseOrderRepo.findByPurchaseOrderIdAndBuyerOrgAccountId(id, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "purchaseOrderId", id));
		// TEMP-ADDRESS-BACKFILL-START: persist org-default addresses onto old
		// POs that were created before addresses existed. TEMPORARY migration
		// logic — delete this block together with the TEMP-ADDRESS-DEFAULTS
		// helpers below once every PO carries its own addresses.
		// NOTE: always the PO's buyer (creator) org, never the viewing
		// caller's org.
		if (isBlank(po.getShippingAddress()) || isBlank(po.getBillingAddress())) {
			boolean touched = false;
			java.util.List<com.fasterxml.jackson.databind.JsonNode> addresses = fetchOrgAddressesQuietly(
					buyerOrgIdOf(po, orgId));
			if (isBlank(po.getShippingAddress())) {
				String resolved = selectOrgAddressText(addresses, true);
				if (resolved != null) {
					po.setShippingAddress(resolved);
					touched = true;
				}
			}
			if (isBlank(po.getBillingAddress())) {
				String resolved = selectOrgAddressText(addresses, false);
				if (resolved != null) {
					po.setBillingAddress(resolved);
					touched = true;
				}
			}
			if (touched) {
				po = purchaseOrderRepo.save(po);
			}
		}
		// TEMP-ADDRESS-BACKFILL-END
		return new ResponseEntity<>(toDto(po), HttpStatus.OK);
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
			pos = purchaseOrderRepo.findByBuyerOrgAccountId(orgId, pageable);
		}
		Page<PurchaseOrderDto> poDtos = pos.map(po -> toDto(po));
		return new ResponseEntity<>(poDtos, HttpStatus.OK);
	}

	@Override
	@Transactional
	public ResponseEntity<?> updatePurchaseOrder(Long id, PurchaseOrderDto poDto) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		PurchaseOrder po = purchaseOrderRepo.findByPurchaseOrderIdAndBuyerOrgAccountId(id, orgId)
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
		po.setShippingAddress(poDto.getShippingAddress());
		po.setBillingAddress(poDto.getBillingAddress());
		po.setIsBlanketOrder(poDto.getIsBlanketOrder());
		po.setBlanketStartDate(poDto.getBlanketStartDate());
		po.setBlanketEndDate(poDto.getBlanketEndDate());
		po.setReleaseSchedule(poDto.getReleaseSchedule());

		// Update line items - replace all
		po.getLineItems().clear();
		if (poDto.getLineItems() != null && !poDto.getLineItems().isEmpty()) {
			for (PurchaseOrderLineItemDto lineDto : poDto.getLineItems()) {
				PurchaseOrderLineItem lineItem = toLineItemEntity(lineDto);
				if (lineItem.getQuantityOrdered() != null && lineItem.getUnitPrice() != null) {
					lineItem.setTotalPrice(lineItem.getQuantityOrdered() * lineItem.getUnitPrice());
				}
				po.addLineItem(lineItem);
			}
		}

		PurchaseOrder savedPo = purchaseOrderRepo.save(po);
		return new ResponseEntity<>(toDto(savedPo), HttpStatus.OK);
	}

	@Override
	@Transactional
	public ResponseEntity<?> deletePurchaseOrder(Long id) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		PurchaseOrder po = purchaseOrderRepo.findByPurchaseOrderIdAndBuyerOrgAccountId(id, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "purchaseOrderId", id));

		// Only allow deletes in DRAFT status
		if (po.getStatus() != PurchaseOrderStatus.DRAFT) {
			throw new ValidationException("Can only delete purchase orders in DRAFT status");
		}

		purchaseOrderRepo.delete(po);
		return ResponseEntity.ok(java.util.Map.of("message", "Purchase order deleted successfully"));
	}

	/**
	 * ModelMapper cannot map PurchaseOrder → PurchaseOrderDto: nested *Id
	 * properties (partnership.partnershipId/invitationId/…, buyerOrg.accountId,
	 * supplier.supplierId/…, line items) are ambiguous for the scalar id
	 * destinations. Map explicitly with ids only — never entity graphs (also
	 * avoids infinite Jackson nesting on reads).
	 */
	// TEMP-ADDRESS-DEFAULTS-START: temporary migration helpers that fill PO
	// shipping/billing addresses from the buyer org's HR addresses. Delete
	// this whole block (and its two call sites marked TEMP-ADDRESS-DEFAULTS /
	// TEMP-ADDRESS-BACKFILL) once every PO is created with explicit
	// addresses — nothing else depends on it.
	/**
	 * The org whose addresses may backfill this PO: always the buyer
	 * (creator) org. Falls back to the caller only when the buyer link is
	 * unexpectedly absent.
	 */
	private static Long buyerOrgIdOf(PurchaseOrder po, Long callerOrgId) {
		if (po.getBuyerOrg() != null && po.getBuyerOrg().getAccountId() != null) {
			return po.getBuyerOrg().getAccountId();
		}
		return callerOrgId;
	}

	private void applyOrgAddressDefaults(PurchaseOrder po, Long orgId) {		if (!isBlank(po.getShippingAddress()) && !isBlank(po.getBillingAddress())) {
			return;
		}
		java.util.List<com.fasterxml.jackson.databind.JsonNode> addresses = fetchOrgAddressesQuietly(orgId);
		if (isBlank(po.getShippingAddress())) {
			String resolved = selectOrgAddressText(addresses, true);
			if (resolved != null) {
				po.setShippingAddress(resolved);
			}
		}
		if (isBlank(po.getBillingAddress())) {
			String resolved = selectOrgAddressText(addresses, false);
			if (resolved != null) {
				po.setBillingAddress(resolved);
			}
		}
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	/**
	 * Pick an address display string: the default-flagged address for the
	 * purpose (shipping/billing) wins; otherwise a lone address — whatever
	 * its flags — serves both purposes; otherwise null (leave blank).
	 */
	private static String selectOrgAddressText(
			java.util.List<com.fasterxml.jackson.databind.JsonNode> addresses, boolean shipping) {
		if (addresses == null || addresses.isEmpty()) {
			return null;
		}
		for (com.fasterxml.jackson.databind.JsonNode a : addresses) {
			boolean flagged = shipping ? isTrue(a.path("isDefaultShipping"))
					: isTrue(a.path("isDefaultBilling"));
			if (flagged) {
				return formatOrgAddress(a);
			}
		}
		if (addresses.size() == 1) {
			return formatOrgAddress(addresses.get(0));
		}
		return null;
	}

	private static boolean isTrue(com.fasterxml.jackson.databind.JsonNode node) {
		return node != null && node.isBoolean() && node.booleanValue();
	}

	private static String formatOrgAddress(com.fasterxml.jackson.databind.JsonNode a) {
		java.util.List<String> parts = new java.util.ArrayList<>();
		addIfPresent(parts, text(a, "label"));
		addIfPresent(parts, text(a, "addressLine1"));
		addIfPresent(parts, text(a, "addressLine2"));
		String cityLine = joinNonBlank(", ", text(a, "city"), text(a, "state"), text(a, "pincode"));
		addIfPresent(parts, cityLine);
		addIfPresent(parts, text(a, "country"));
		String contact = joinNonBlank(" ", text(a, "contactName"), text(a, "contactPhone"));
		String joined = String.join(", ", parts);
		if (contact.isEmpty()) {
			return joined;
		}
		return joined.isEmpty() ? ("Contact: " + contact) : (joined + " (Contact: " + contact + ")");
	}

	private static String text(com.fasterxml.jackson.databind.JsonNode a, String field) {
		if (a == null || !a.has(field) || a.path(field).isNull()) {
			return "";
		}
		String value = a.path(field).asText("");
		return value == null ? "" : value.trim();
	}

	private static void addIfPresent(java.util.List<String> parts, String value) {
		if (value != null && !value.isEmpty()) {
			parts.add(value);
		}
	}

	private static String joinNonBlank(String delimiter, String... values) {
		java.util.List<String> kept = new java.util.ArrayList<>();
		for (String value : values) {
			if (value != null && !value.isEmpty()) {
				kept.add(value);
			}
		}
		return String.join(delimiter, kept);
	}

	/**
	 * Best-effort fetch of the org's HR addresses. Never throws: HR being
	 * down must not break PO creation or reads — the PO simply keeps blank
	 * addresses in that case.
	 */
	private java.util.List<com.fasterxml.jackson.databind.JsonNode> fetchOrgAddressesQuietly(Long orgId) {
		java.util.List<com.fasterxml.jackson.databind.JsonNode> empty = java.util.Collections.emptyList();
		try {
			String url = webConstants.getHrOrgAddressesBaseUrl() + "/" + orgId + "/addresses";
			String token = OrganizationContextHolder.getCurrentAuthToken();
			java.util.Map<String, String> headers = new java.util.HashMap<>();
			headers.put("Content-Type", "application/json");
			if (token != null && !token.isBlank()) {
				headers.put("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);
			}
			org.springframework.http.ResponseEntity<String> response = restService.coreRestCall(url, null, headers,
					org.springframework.http.HttpMethod.GET, orgId);
			if (response == null || !response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
				return empty;
			}
			com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper()
					.readTree(response.getBody());
			java.util.List<com.fasterxml.jackson.databind.JsonNode> out = new java.util.ArrayList<>();
			if (root != null && root.isArray()) {
				root.forEach(out::add);
			}
			return out;
		} catch (Exception e) {
			log.warn("Skipping org address defaults for org {}: {}", orgId, e.getMessage());
			return empty;
		}
	}
	// TEMP-ADDRESS-DEFAULTS-END

	public static PurchaseOrderDto toDto(PurchaseOrder po) {
		PurchaseOrderDto dto = new PurchaseOrderDto();
		dto.setPurchaseOrderId(po.getPurchaseOrderId());
		dto.setPoNumber(po.getPoNumber());
		dto.setPurchaseOrderNumber(po.getPoNumber());
		dto.setOrderDate(po.getCreatedAt());
		dto.setRevisionNumber(po.getRevisionNumber());
		dto.setParentPoId(po.getParentPoId());
		dto.setBuyerOrgId(po.getBuyerOrg() != null ? po.getBuyerOrg().getAccountId() : null);
		dto.setBuyerOrgName(po.getBuyerOrg() != null ? po.getBuyerOrg().getName() : null);
		dto.setSupplierId(po.getSupplier() != null ? po.getSupplier().getSupplierId() : null);
		dto.setSupplierName(po.getSupplier() != null ? po.getSupplier().getBusinessName() : null);
		dto.setSupplierOrgId(po.getSupplierOrg() != null ? po.getSupplierOrg().getAccountId() : null);
		String supplierOrgName = po.getSupplierOrg() != null ? po.getSupplierOrg().getName() : null;
		if ((supplierOrgName == null || supplierOrgName.isBlank()) && po.getSupplier() != null) {
			supplierOrgName = po.getSupplier().getBusinessName();
		}
		dto.setSupplierOrgName(supplierOrgName);
		dto.setPartnershipId(po.getPartnership() != null ? po.getPartnership().getPartnershipId() : null);
		dto.setSourceQuotationId(
				po.getSourceQuotation() != null ? po.getSourceQuotation().getQuotationId() : null);
		dto.setSourceQuotationNumber(
				po.getSourceQuotation() != null ? po.getSourceQuotation().getQuotationNumber() : null);
		dto.setStatus(po.getStatus());
		dto.setTotalAmount(po.getTotalAmount());
		dto.setCurrency(po.getCurrency());
		dto.setPaymentTerms(po.getPaymentTerms());
		dto.setIncoterms(po.getIncoterms());
		dto.setRequestedDeliveryDate(po.getRequestedDeliveryDate());
		dto.setExpectedDeliveryDate(po.getExpectedDeliveryDate());
		dto.setNotes(po.getNotes());
		dto.setShippingAddress(po.getShippingAddress());
		dto.setBillingAddress(po.getBillingAddress());
		dto.setIsBlanketOrder(po.getIsBlanketOrder());
		dto.setBlanketStartDate(po.getBlanketStartDate());
		dto.setBlanketEndDate(po.getBlanketEndDate());
		dto.setReleaseSchedule(po.getReleaseSchedule());
		dto.setApprovedBy(po.getApprovedBy());
		dto.setRejectionReason(po.getRejectionReason());
		dto.setApprovalLevel(po.getApprovalLevel());
		dto.setRequiredApproverLevel(po.getRequiredApproverLevel());
		dto.setCurrentApprover(po.getCurrentApprover());
		dto.setApprovalDelegatedTo(po.getApprovalDelegatedTo());
		if (po.getLineItems() != null) {
			java.util.List<PurchaseOrderLineItemDto> lines = new java.util.ArrayList<>(po.getLineItems().size());
			for (PurchaseOrderLineItem line : po.getLineItems()) {
				PurchaseOrderLineItemDto lineDto = new PurchaseOrderLineItemDto();
				lineDto.setLineItemId(line.getLineItemId());
				lineDto.setLineNumber(line.getLineNumber());
				lineDto.setMaterialId(line.getMaterial() != null ? line.getMaterial().getMaterialId() : null);
				lineDto.setProductId(line.getProduct() != null ? line.getProduct().getProductId() : null);
				lineDto.setDescription(line.getDescription());
				lineDto.setQuantityOrdered(line.getQuantityOrdered());
				lineDto.setQuantityReceived(line.getQuantityReceived());
				lineDto.setQuantityInvoiced(line.getQuantityInvoiced());
				lineDto.setUnitPrice(line.getUnitPrice());
				lineDto.setTotalPrice(line.getTotalPrice());
				lineDto.setUnitOfMeasure(line.getUnitOfMeasure());
				lineDto.setIncoterms(line.getIncoterms());
				lineDto.setDeliveryLocation(line.getDeliveryLocation());
				lineDto.setCatalogId(line.getCatalogId());
				lines.add(lineDto);
			}
			dto.setLineItems(lines);
		}
		return dto;
	}

	/**
	 * ModelMapper cannot map PurchaseOrderLineItemDto → PurchaseOrderLineItem:
	 * the *Id fields (lineItemId/productId/materialId/catalogId) all match the
	 * nested destination purchaseOrder.purchaseOrderId. Map scalars explicitly;
	 * material/product links are resolved by the caller.
	 */
	private PurchaseOrderLineItem toLineItemEntity(PurchaseOrderLineItemDto lineDto) {
		PurchaseOrderLineItem lineItem = new PurchaseOrderLineItem();
		lineItem.setLineItemId(lineDto.getLineItemId());
		lineItem.setLineNumber(lineDto.getLineNumber());
		lineItem.setDescription(lineDto.getDescription());
		lineItem.setQuantityOrdered(lineDto.getQuantityOrdered());
		lineItem.setQuantityReceived(lineDto.getQuantityReceived());
		lineItem.setQuantityInvoiced(lineDto.getQuantityInvoiced());
		lineItem.setUnitPrice(lineDto.getUnitPrice());
		lineItem.setTotalPrice(lineDto.getTotalPrice());
		lineItem.setUnitOfMeasure(lineDto.getUnitOfMeasure());
		lineItem.setIncoterms(lineDto.getIncoterms());
		lineItem.setDeliveryLocation(lineDto.getDeliveryLocation());
		lineItem.setCatalogId(lineDto.getCatalogId());
		return lineItem;
	}

	@Override
	@Transactional
	public ResponseEntity<?> transitionStatus(Long id, PurchaseOrderStatus newStatus, Map<String, Object> params) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		PurchaseOrder po = purchaseOrderRepo.findByPurchaseOrderIdAndBuyerOrgAccountId(id, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "purchaseOrderId", id));

		PurchaseOrderStatus currentStatus = po.getStatus();

		// Validate state transition
		validateTransition(po, currentStatus, newStatus, params);

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
		return new ResponseEntity<>(toDto(savedPo), HttpStatus.OK);
	}

		PurchaseOrder savedPo = purchaseOrderRepo.save(po);
		return new ResponseEntity<>(toDto(savedPo), HttpStatus.OK);
	}

	private void validateTransition(PurchaseOrder po, PurchaseOrderStatus currentStatus, PurchaseOrderStatus newStatus,
			Map<String, Object> params) {
		// Define valid transitions
		switch (currentStatus) {
			case DRAFT:
				if (newStatus != PurchaseOrderStatus.PENDING_APPROVAL && newStatus != PurchaseOrderStatus.CANCELLED) {
					throw new ValidationException("From DRAFT, can only transition to PENDING_APPROVAL or CANCELLED");
				}
				if (newStatus == PurchaseOrderStatus.PENDING_APPROVAL) {
					// The PO's stored line items count — callers are not
					// required to resend them in the transition body.
					if (po.getLineItems() == null || po.getLineItems().isEmpty()) {
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
						&& newStatus != PurchaseOrderStatus.AWAITING_PICKUP
						&& newStatus != PurchaseOrderStatus.CANCELLED) {
					throw new ValidationException(
							"From ACKNOWLEDGED, can only transition to PARTIALLY_RECEIVED, RECEIVED, AWAITING_PICKUP, or CANCELLED");
				}
				break;
			case AWAITING_PICKUP:
				if (newStatus != PurchaseOrderStatus.PICKED_UP && newStatus != PurchaseOrderStatus.CANCELLED) {
					throw new ValidationException(
							"From AWAITING_PICKUP, can only transition to PICKED_UP or CANCELLED");
				}
				break;
			case PICKED_UP:
				if (newStatus != PurchaseOrderStatus.PARTIALLY_RECEIVED && newStatus != PurchaseOrderStatus.RECEIVED
						&& newStatus != PurchaseOrderStatus.CANCELLED) {
					throw new ValidationException(
							"From PICKED_UP, can only transition to PARTIALLY_RECEIVED, RECEIVED, or CANCELLED");
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
	 * Integrates with HR service to check user's role/level in organizational
	 * hierarchy.
	 */
	private boolean hasApprovalAuthority(String userId, ApprovalLevel requiredLevel) {
		if (requiredLevel == ApprovalLevel.AUTO) {
			return true; // Auto approval doesn't require human authority
		}

		try {
			// Call HR service to check approval authority
			String url = webConstants.getHrServiceUrl() + webConstants.getHrEmployeeApprovalAuthorityUrl();
			Map<String, Object> request = Map.of(
					"employeeId", userId,
					"requiredLevel", requiredLevel.name());

			Long orgId = OrganizationContextHolder.requireOrganizationId();
			Map<String, String> headers = Map.of("Content-Type", "application/json");

			ResponseEntity<Map> response = restService.post(url, request, headers, Map.class, orgId);

			if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
				Boolean hasAuthority = (Boolean) response.getBody().get("hasAuthority");
				return Boolean.TRUE.equals(hasAuthority);
			}

			log.warn("HR service returned non-success status for approval authority check: {}",
					response.getStatusCode());
			return false;
		} catch (Exception e) {
			log.error("Error checking approval authority with HR service for user {}: {}", userId, e.getMessage());
			// Fail closed - deny approval if HR service is unavailable
			return false;
		}
	}

	@Override
	@Transactional
	public ResponseEntity<?> createAmendment(Long parentPoId, PurchaseOrderDto amendmentDto) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		PurchaseOrder parentPo = purchaseOrderRepo.findByPurchaseOrderIdAndBuyerOrgAccountId(parentPoId, orgId)
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
				.map(po -> toDto(po))
				.collect(Collectors.toList());
		return new ResponseEntity<>(amendmentDtos, HttpStatus.OK);
	}

	/**
	 * Process blanket order releases based on schedule.
	 * FR-RET-004: Blanket order support with release scheduling.
	 * Creates release POs based on the blanket order's release schedule.
	 */
	@Override
	@Transactional
	public ResponseEntity<?> processBlanketOrderReleases(Long blanketPoId) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		PurchaseOrder blanketPo = purchaseOrderRepo.findByPurchaseOrderIdAndBuyerOrgAccountId(blanketPoId, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "purchaseOrderId", blanketPoId));

		if (!blanketPo.isBlanketOrder()) {
			throw new ValidationException("PO is not a blanket order");
		}

		if (blanketPo.getBlanketStartDate() == null || blanketPo.getBlanketEndDate() == null) {
			throw new ValidationException("Blanket order must have start and end dates");
		}

		if (blanketPo.getReleaseSchedule() == null || blanketPo.getReleaseSchedule().trim().isEmpty()) {
			throw new ValidationException("Blanket order must have a release schedule");
		}

		// Parse release schedule (supports cron expression or simple frequency)
		List<LocalDateTime> releaseDates = parseReleaseSchedule(
				blanketPo.getReleaseSchedule(),
				blanketPo.getBlanketStartDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime(),
				blanketPo.getBlanketEndDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());

		List<PurchaseOrderDto> releasePos = new ArrayList<>();
		for (LocalDateTime releaseDate : releaseDates) {
			if (releaseDate.isAfter(LocalDateTime.now())) {
				// Create release PO
				PurchaseOrderDto releaseDto = toDto(blanketPo);
				releaseDto.setPurchaseOrderId(null); // New PO
				releaseDto.setParentPoId(blanketPoId);
				releaseDto.setRevisionNumber(blanketPo.getRevisionNumber() + 1);
				releaseDto.setPoNumber(blanketPo.getPoNumber() + "-R" + (blanketPo.getRevisionNumber() + 1));
				releaseDto.setStatus(PurchaseOrderStatus.DRAFT);
				releaseDto.setIsBlanketOrder(false);
				releaseDto.setBlanketStartDate(null);
				releaseDto.setBlanketEndDate(null);
				releaseDto.setReleaseSchedule(null);
				releaseDto.setExpectedDeliveryDate(java.sql.Date.valueOf(releaseDate.toLocalDate()));

				ResponseEntity<?> response = createPurchaseOrder(releaseDto);
				if (response.getStatusCode().is2xxSuccessful()) {
					releasePos.add((PurchaseOrderDto) response.getBody());
				}
			}
		}

		return new ResponseEntity<>(releasePos, HttpStatus.OK);
	}

	/**
	 * Parse release schedule to generate release dates.
	 * Supports cron expressions and simple frequency patterns.
	 */
	private List<LocalDateTime> parseReleaseSchedule(String schedule, LocalDateTime startDate, LocalDateTime endDate) {
		List<LocalDateTime> dates = new ArrayList<>();

		// Simple frequency parsing (e.g., "WEEKLY", "MONTHLY", "QUARTERLY", "DAILY")
		// For cron expressions, a full cron parser would be needed
		String upperSchedule = schedule.trim().toUpperCase();

		LocalDateTime current = startDate;
		switch (upperSchedule) {
			case "DAILY":
				while (current.isBefore(endDate) || current.isEqual(endDate)) {
					dates.add(current);
					current = current.plusDays(1);
				}
				break;
			case "WEEKLY":
				while (current.isBefore(endDate) || current.isEqual(endDate)) {
					dates.add(current);
					current = current.plusWeeks(1);
				}
				break;
			case "BIWEEKLY":
				while (current.isBefore(endDate) || current.isEqual(endDate)) {
					dates.add(current);
					current = current.plusWeeks(2);
				}
				break;
			case "MONTHLY":
				while (current.isBefore(endDate) || current.isEqual(endDate)) {
					dates.add(current);
					current = current.plusMonths(1);
				}
				break;
			case "QUARTERLY":
				while (current.isBefore(endDate) || current.isEqual(endDate)) {
					dates.add(current);
					current = current.plusMonths(3);
				}
				break;
			default:
				// Try to parse as cron expression (simplified)
				log.warn("Unrecognized release schedule format: {}. Treating as MONTHLY.", schedule);
				while (current.isBefore(endDate) || current.isEqual(endDate)) {
					dates.add(current);
					current = current.plusMonths(1);
				}
				break;
		}

		return dates;
	}

	/**
	 * Get all blanket orders for the organization.
	 */
	@Override
	public ResponseEntity<?> getBlanketOrders(Pageable pageable) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Page<PurchaseOrder> blanketOrders = purchaseOrderRepo.findByBuyerOrgAccountIdAndIsBlanketOrderTrue(orgId,
				pageable);
		Page<PurchaseOrderDto> dtoPage = blanketOrders.map(po -> toDto(po));
		return new ResponseEntity<>(dtoPage, HttpStatus.OK);
	}

	/**
	 * Get release POs for a blanket order.
	 */
	@Override
	public ResponseEntity<?> getBlanketOrderReleases(Long blanketPoId) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		List<PurchaseOrder> releases = purchaseOrderRepo.findByParentPoIdAndIsBlanketOrderFalse(orgId, blanketPoId);
		List<PurchaseOrderDto> releaseDtos = releases.stream()
				.map(po -> toDto(po))
				.collect(Collectors.toList());
		return new ResponseEntity<>(releaseDtos, HttpStatus.OK);
	}
}