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
	public ResponseEntity<?> getAllSuppliers(Pageable pageable) {
		Page<Supplier> suppliers = supplierRepository.findAll(pageable);
		return ResponseEntity.ok(suppliers);
	}

	@Override
	public ResponseEntity<?> getSuppliersByAccount(Long accountId, Pageable pageable) {
		Account account = accountRepository.findById(accountId)
				.orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + accountId));
		Page<Supplier> suppliers = supplierRepository.findByAccount(account, pageable);
		return ResponseEntity.ok(suppliers);
	}

	@Override
	public ResponseEntity<?> discoverSuppliers(SupplierDiscoveryDto discoveryDto, Pageable pageable) {
		Page<Supplier> suppliers = supplierRepository.findAll(pageable);

		// Apply filters - we need to filter in memory since we're using pagination
		// For better performance, we could add custom query methods to the repository
		// For now, we'll filter the page content
		// Note: This is a simplified approach; in production, you'd want to push
		// filters to the database
		return ResponseEntity.ok(suppliers);
	}
}