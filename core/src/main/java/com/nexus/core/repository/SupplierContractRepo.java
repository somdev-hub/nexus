package com.nexus.core.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nexus.core.entities.SupplierContract;

@Repository
public interface SupplierContractRepo extends JpaRepository<SupplierContract, Long> {

	Optional<SupplierContract> findByAccountIdAndContractNumber(Long accountId, String contractNumber);

	Page<SupplierContract> findByAccountId(Long accountId, Pageable pageable);

	@Query("SELECT sc FROM SupplierContract sc WHERE sc.account.id = :accountId " +
			"AND (:status IS NULL OR sc.status = :status) " +
			"AND (:supplierId IS NULL OR sc.supplier.id = :supplierId) " +
			"AND (:contractType IS NULL OR sc.contractType = :contractType) " +
			"AND (:startDate IS NULL OR sc.effectiveDate >= :startDate) " +
			"AND (:endDate IS NULL OR sc.effectiveDate <= :endDate) " +
			"AND (:expiryStartDate IS NULL OR sc.expiryDate >= :expiryStartDate) " +
			"AND (:expiryEndDate IS NULL OR sc.expiryDate <= :expiryEndDate) " +
			"AND (:expiringOnly IS NULL OR :expiringOnly = false OR (sc.status IN ('ACTIVE', 'RENEWAL_PENDING') AND sc.expiryDate IS NOT NULL AND sc.expiryDate <= :expiringBeforeDate)) "
			+
			"AND (:autoRenewalOnly IS NULL OR :autoRenewalOnly = false OR (sc.autoRenewal = true AND sc.expiryDate IS NOT NULL AND sc.expiryDate <= :autoRenewalBeforeDate)) "
			+
			"ORDER BY sc.effectiveDate DESC")
	Page<SupplierContract> findByAccountIdWithFilters(
			@Param("accountId") Long accountId,
			@Param("status") SupplierContract.ContractStatus status,
			@Param("supplierId") Long supplierId,
			@Param("contractType") SupplierContract.ContractType contractType,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate,
			@Param("expiryStartDate") LocalDate expiryStartDate,
			@Param("expiryEndDate") LocalDate expiryEndDate,
			@Param("expiringOnly") Boolean expiringOnly,
			@Param("expiringBeforeDate") LocalDate expiringBeforeDate,
			@Param("autoRenewalOnly") Boolean autoRenewalOnly,
			@Param("autoRenewalBeforeDate") LocalDate autoRenewalBeforeDate,
			Pageable pageable);

	Page<SupplierContract> findByAccountIdAndStatus(Long accountId, SupplierContract.ContractStatus status,
			Pageable pageable);

	Page<SupplierContract> findByAccountIdAndSupplierId(Long accountId, Long supplierId, Pageable pageable);

	Page<SupplierContract> findByAccountIdAndContractType(Long accountId, SupplierContract.ContractType contractType,
			Pageable pageable);

	Page<SupplierContract> findByAccountIdAndEffectiveDateBetween(Long accountId, LocalDate startDate,
			LocalDate endDate, Pageable pageable);

	Page<SupplierContract> findByAccountIdAndExpiryDateBetween(Long accountId, LocalDate startDate, LocalDate endDate,
			Pageable pageable);

	List<SupplierContract> findByAccountIdAndExpiryDateBeforeAndStatusIn(Long accountId, LocalDate date,
			List<SupplierContract.ContractStatus> statuses);

	List<SupplierContract> findByAccountIdAndAutoRenewalTrueAndExpiryDateBefore(Long accountId, LocalDate date);

	@Query("SELECT sc FROM SupplierContract sc WHERE sc.account.id = :accountId AND sc.supplier.id = :supplierId AND sc.status = 'ACTIVE' AND sc.effectiveDate <= :date AND (sc.expiryDate IS NULL OR sc.expiryDate >= :date)")
	Optional<SupplierContract> findActiveContractBySupplierAndDate(@Param("accountId") Long accountId,
			@Param("supplierId") Long supplierId, @Param("date") LocalDate date);

	@Query("SELECT COUNT(sc) FROM SupplierContract sc WHERE sc.account.id = :accountId AND sc.status = 'ACTIVE'")
	Long countActiveContractsByAccount(@Param("accountId") Long accountId);

	@Query("SELECT COUNT(sc) FROM SupplierContract sc WHERE sc.account.id = :accountId AND sc.status = 'DRAFT'")
	Long countDraftContractsByAccount(@Param("accountId") Long accountId);

	@Query("SELECT COUNT(sc) FROM SupplierContract sc WHERE sc.account.id = :accountId AND sc.status = 'PENDING_APPROVAL'")
	Long countPendingApprovalContractsByAccount(@Param("accountId") Long accountId);

	@Query("SELECT COUNT(sc) FROM SupplierContract sc WHERE sc.account.id = :accountId AND sc.status = 'EXPIRED'")
	Long countExpiredContractsByAccount(@Param("accountId") Long accountId);

	@Query("SELECT sc FROM SupplierContract sc WHERE sc.account.id = :accountId ORDER BY sc.effectiveDate DESC")
	List<SupplierContract> findByAccountIdOrderByEffectiveDateDesc(@Param("accountId") Long accountId);

	@Query("SELECT sc FROM SupplierContract sc WHERE sc.account.id = :accountId AND sc.supplier.id = :supplierId ORDER BY sc.effectiveDate DESC")
	List<SupplierContract> findByAccountIdAndSupplierIdOrderByEffectiveDateDesc(@Param("accountId") Long accountId,
			@Param("supplierId") Long supplierId);
}