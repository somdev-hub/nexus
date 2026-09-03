package com.nexus.core.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nexus.core.entities.Account;
import com.nexus.core.entities.Supplier;
import com.nexus.core.entities.SupplierStatus;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

	Page<Supplier> findAll(Pageable pageable);

	Page<Supplier> findByStatus(SupplierStatus status, Pageable pageable);

	Page<Supplier> findByCategory(String category, Pageable pageable);

	Page<Supplier> findByLocation(String location, Pageable pageable);

	Page<Supplier> findByRatingGreaterThanEqual(Double rating, Pageable pageable);

	@Query("SELECT s FROM Supplier s WHERE s.certifications LIKE CONCAT('%', :certification, '%')")
	Page<Supplier> findByCertification(@Param("certification") String certification, Pageable pageable);

	Page<Supplier> findByAccount(Account account, Pageable pageable);

	Optional<Supplier> findBySupplierId(Long supplierId);

	Optional<Supplier> findBySupplierIdAndAccountAccountId(Long supplierId, Long accountId);

	Optional<Supplier> findBySupplierIdAndAccountAccountIdAndIsActiveTrue(Long supplierId, Long accountId);

	// New methods for organization-scoped queries
	Page<Supplier> findByAccountAccountId(Long accountId, Pageable pageable);

	Page<Supplier> findByCategoryAndAccountAccountId(String category, Long accountId, Pageable pageable);

	Page<Supplier> findByLocationAndAccountAccountId(String location, Long accountId, Pageable pageable);

	Page<Supplier> findByRatingGreaterThanEqualAndAccountAccountId(Double rating, Long accountId, Pageable pageable);

	@Query("SELECT s FROM Supplier s WHERE s.certifications LIKE CONCAT('%', :certification, '%') AND s.account.accountId = :accountId")
	Page<Supplier> findByCertificationAndAccountAccountId(@Param("certification") String certification,
			@Param("accountId") Long accountId, Pageable pageable);
}