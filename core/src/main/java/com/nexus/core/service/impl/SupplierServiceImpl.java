package com.nexus.core.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.nexus.core.entities.Account;
import com.nexus.core.entities.Supplier;
import com.nexus.core.entities.SupplierStatus;
import com.nexus.core.payload.SupplierDto;
import com.nexus.core.payload.SupplierDiscoveryDto;
import com.nexus.core.repository.AccountRepository;
import com.nexus.core.repository.SupplierRepository;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.SupplierService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

	private final SupplierRepository supplierRepository;
	private final AccountRepository accountRepository;

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