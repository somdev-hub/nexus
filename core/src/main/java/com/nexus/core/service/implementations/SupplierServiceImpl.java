package com.nexus.core.service.implementations;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.nexus.core.model.entities.Account;
import com.nexus.core.model.entities.Supplier;
import com.nexus.core.model.enums.SupplierStatus;
import com.nexus.core.payload.SupplierDto;
import com.nexus.core.payload.SupplierDiscoveryDto;
import com.nexus.core.repository.AccountRepository;
import com.nexus.core.repository.SupplierRepository;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.interfaces.SupplierService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

	private final SupplierRepository supplierRepository;
	private final AccountRepository accountRepository;
	private final com.nexus.core.repository.PartnershipRepo partnershipRepo;
	private final com.nexus.core.repository.PartnershipInvitationRepo invitationRepo;

	@Override
	public ResponseEntity<?> addSupplier(SupplierDto supplierDto) {
		Account account = accountRepository.findById(supplierDto.getAccountId())
				.orElseThrow(
						() -> new IllegalArgumentException("Account not found with ID: " + supplierDto.getAccountId()));

		Supplier supplier = new Supplier();
		supplier.setAccount(account);
		supplier.setBusinessName(supplierDto.getBusinessName());
		supplier.setCategory(supplierDto.getCategory());
		supplier.setLocation(supplierDto.getLocation());
		supplier.setWebsite(supplierDto.getWebsite());
		supplier.setContactPerson(supplierDto.getContactPerson());
		supplier.setContactEmail(supplierDto.getContactEmail());
		supplier.setContactPhone(supplierDto.getContactPhone());
		supplier.setCertifications(supplierDto.getCertifications());
		supplier.setStatus(SupplierStatus.PENDING_VERIFICATION);
		supplier.setRating(0.0);
		supplier.setTotalOrders(0);
		supplier.setOnTimeDeliveryRate(0.0);
		supplier.setQualityScore(0.0);

		Supplier saved = supplierRepository.save(supplier);
		return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	}

	@Override
	public ResponseEntity<?> getSupplierById(Long id) {
		return supplierRepository.findById(id)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<?> getAllSuppliers(Long accountId, String category, String location, Double minRating,
			String certification, Pageable pageable) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		// Backfill: every ACTIVE supplier-type partnership the caller (retailer)
		// is party to must have a local Supplier row, otherwise the PO
		// supplier dropdown stays empty even though partnerships exist.
		ensurePartnershipSuppliers(orgId);

		// If accountId is provided, validate it belongs to the organization
		if (accountId != null) {
			Account account = accountRepository.findById(accountId)
					.orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + accountId));
			if (!account.getAccountId().equals(orgId)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied to this account");
			}
			Page<Supplier> suppliers = supplierRepository.findByAccount(account, pageable);
			return ResponseEntity.ok(suppliers);
		}

		// Build dynamic query based on filters
		if (category != null && !category.isBlank()) {
			return ResponseEntity.ok(supplierRepository.findByCategoryAndAccountAccountId(category, orgId, pageable));
		}
		if (location != null && !location.isBlank()) {
			return ResponseEntity.ok(supplierRepository.findByLocationAndAccountAccountId(location, orgId, pageable));
		}
		if (minRating != null) {
			return ResponseEntity
					.ok(supplierRepository.findByRatingGreaterThanEqualAndAccountAccountId(minRating, orgId, pageable));
		}
		if (certification != null && !certification.isBlank()) {
			return ResponseEntity
					.ok(supplierRepository.findByCertificationAndAccountAccountId(certification, orgId, pageable));
		}

		// No filters - return all suppliers for the organization
		Page<Supplier> suppliers = supplierRepository.findByAccountAccountId(orgId, pageable);
		return ResponseEntity.ok(suppliers);
	}

	/**
	 * Ensure a local Supplier row exists (under the caller's account) for every
	 * ACTIVE supplier-type partnership the caller is party to. Only applies to
	 * retailer callers — other org types skip. Idempotent: existing rows are
	 * reused (blank business names are refreshed when the stored partnership
	 * org name is known).
	 */
	private void ensurePartnershipSuppliers(Long orgId) {
		String orgType = OrganizationContextHolder.getCurrentOrganizationType();
		if (orgType == null || !"RETAILER".equalsIgnoreCase(orgType)) {
			return;
		}
		java.util.List<com.nexus.core.model.entities.Partnership> partnerships = new java.util.ArrayList<>();
		partnerships.addAll(
				partnershipRepo.findByPrimaryOrgAccountId(orgId, Pageable.unpaged()).getContent());
		partnerships.addAll(
				partnershipRepo.findBySecondaryOrgAccountId(orgId, Pageable.unpaged()).getContent());
		for (com.nexus.core.model.entities.Partnership partnership : partnerships) {
			// New partnerships start as DRAFT and become ACTIVE; skip only
			// dead ones (terminated / rejected / expired).
			if (partnership.getStatus() == com.nexus.core.model.enums.PartnershipStatus.TERMINATED
					|| partnership.getStatus() == com.nexus.core.model.enums.PartnershipStatus.REJECTED
					|| partnership.getStatus() == com.nexus.core.model.enums.PartnershipStatus.EXPIRED) {
				continue;
			}
			String type = partnership.getPartnershipType();
			if (type == null || type.isBlank()) {
				// Legacy rows predate partnershipType: derive it from the
				// originating invitation's context and backfill it.
				type = resolveTypeFromInvitation(partnership);
				if (type != null) {
					partnership.setPartnershipType(type);
					try {
						partnershipRepo.save(partnership);
					} catch (Exception ignored) {
					}
				}
			}
			if (type == null || !"SUPPLIER".equalsIgnoreCase(type)) {
				continue;
			}
			boolean isPrimary = partnership.getPrimaryOrg() != null
					&& orgId.equals(partnership.getPrimaryOrg().getAccountId());
			com.nexus.core.model.entities.Account counterparty = isPrimary
					? partnership.getSecondaryOrg()
					: partnership.getPrimaryOrg();
			if (counterparty == null || counterparty.getAccountId() == null) {
				continue;
			}
			String counterpartyName = isPrimary
					? partnership.getSecondaryOrgName()
					: partnership.getPrimaryOrgName();
			if (counterpartyName == null || counterpartyName.isBlank()) {
				counterpartyName = counterparty.getName();
			}
			java.util.Optional<Supplier> existing = supplierRepository
					.findByAccountAccountIdAndSupplierOrgAccountId(orgId, counterparty.getAccountId());
			if (existing.isPresent()) {
				Supplier row = existing.get();
				if ((row.getBusinessName() == null || row.getBusinessName().isBlank())
						&& counterpartyName != null && !counterpartyName.isBlank()) {
					row.setBusinessName(counterpartyName);
					supplierRepository.save(row);
				}
				continue;
			}
			Account owner = accountRepository.findById(orgId).orElse(null);
			if (owner == null) {
				continue;
			}
			Supplier row = new Supplier();
			row.setAccount(owner);
			row.setBusinessName(counterpartyName);
			row.setStatus(SupplierStatus.ACTIVE);
			row.setRating(0.0);
			row.setTotalOrders(0);
			row.setOnTimeDeliveryRate(0.0);
			row.setQualityScore(0.0);
			row.setSupplierOrgAccountId(counterparty.getAccountId());
			supplierRepository.save(row);
		}
	}

	/**
	 * Derive the partnership type (SUPPLIER / LOGISTICS) from the originating
	 * invitation's partnershipContext for rows created before partnershipType
	 * was stored. Returns null when it cannot be determined.
	 */
	private String resolveTypeFromInvitation(com.nexus.core.model.entities.Partnership partnership) {
		if (partnership.getInvitationId() == null) {
			return null;
		}
		try {
			return invitationRepo.findByInvitationId(partnership.getInvitationId())
					.map(inv -> {
						String context = inv.getPartnershipContext();
						if (context == null) {
							return null;
						}
						if (context.endsWith("LOGISTICS")) {
							return "LOGISTICS";
						}
						if (context.endsWith("SUPPLIER")) {
							return "SUPPLIER";
						}
						return context;
					})
					.orElse(null);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public ResponseEntity<?> discoverSuppliers(SupplierDiscoveryDto discoveryDto, Pageable pageable) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		// Build dynamic query based on filters
		if (discoveryDto.getCategory() != null && !discoveryDto.getCategory().isBlank()) {
			return ResponseEntity.ok(
					supplierRepository.findByCategoryAndAccountAccountId(discoveryDto.getCategory(), orgId, pageable));
		}
		if (discoveryDto.getLocation() != null && !discoveryDto.getLocation().isBlank()) {
			return ResponseEntity.ok(
					supplierRepository.findByLocationAndAccountAccountId(discoveryDto.getLocation(), orgId, pageable));
		}
		if (discoveryDto.getMinRating() != null) {
			return ResponseEntity.ok(supplierRepository
					.findByRatingGreaterThanEqualAndAccountAccountId(discoveryDto.getMinRating(), orgId, pageable));
		}
		if (discoveryDto.getCertifications() != null && !discoveryDto.getCertifications().isBlank()) {
			return ResponseEntity.ok(supplierRepository
					.findByCertificationAndAccountAccountId(discoveryDto.getCertifications(), orgId, pageable));
		}
		if (discoveryDto.getCertificationList() != null && !discoveryDto.getCertificationList().isEmpty()) {
			// For multiple certifications, we'll use the first one for now
			return ResponseEntity.ok(supplierRepository.findByCertificationAndAccountAccountId(
					discoveryDto.getCertificationList().get(0), orgId, pageable));
		}

		// No filters - return all suppliers for the organization
		Page<Supplier> suppliers = supplierRepository.findByAccountAccountId(orgId, pageable);
		return ResponseEntity.ok(suppliers);
	}
}