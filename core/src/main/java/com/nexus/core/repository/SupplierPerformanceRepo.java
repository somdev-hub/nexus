package com.nexus.core.repository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nexus.core.entities.SupplierPerformance;

public interface SupplierPerformanceRepo extends JpaRepository<SupplierPerformance, Long> {

	Optional<List<SupplierPerformance>> findByAccountId(Long accountId);

	Optional<List<SupplierPerformance>> findBySupplierId(Long supplierId);

	Optional<List<SupplierPerformance>> findByAccountIdAndSupplierId(Long accountId, Long supplierId);

	Optional<SupplierPerformance> findByPerformanceIdAndAccountId(Long performanceId, Long accountId);

	Optional<SupplierPerformance> findByPerformanceIdAndSupplierId(Long performanceId, Long supplierId);

	@Query("SELECT p FROM SupplierPerformance p WHERE p.account.accountId = :accountId AND p.evaluationPeriodStart >= :startDate AND p.evaluationPeriodEnd <= :endDate")
	Optional<List<SupplierPerformance>> findByAccountIdAndPeriod(@Param("accountId") Long accountId,
			@Param("startDate") Date startDate, @Param("endDate") Date endDate);

	@Query("SELECT p FROM SupplierPerformance p WHERE p.supplier.supplierId = :supplierId AND p.evaluationPeriodStart >= :startDate AND p.evaluationPeriodEnd <= :endDate")
	Optional<List<SupplierPerformance>> findBySupplierIdAndPeriod(@Param("supplierId") Long supplierId,
			@Param("startDate") Date startDate, @Param("endDate") Date endDate);

	@Query("SELECT p FROM SupplierPerformance p WHERE p.account.accountId = :accountId AND p.performanceTier = :tier")
	Optional<List<SupplierPerformance>> findByAccountIdAndTier(@Param("accountId") Long accountId,
			@Param("tier") SupplierPerformance.PerformanceTier tier);

	@Query("SELECT p FROM SupplierPerformance p WHERE p.supplier.supplierId = :supplierId ORDER BY p.evaluationPeriodEnd DESC")
	Optional<SupplierPerformance> findLatestBySupplierId(@Param("supplierId") Long supplierId);

	@Query("SELECT p FROM SupplierPerformance p WHERE p.account.accountId = :accountId ORDER BY p.evaluationPeriodEnd DESC")
	Page<SupplierPerformance> findByAccountIdOrderByPeriodDesc(@Param("accountId") Long accountId, Pageable pageable);

	@Query("SELECT p FROM SupplierPerformance p WHERE p.supplier.supplierId = :supplierId ORDER BY p.evaluationPeriodEnd DESC")
	Page<SupplierPerformance> findBySupplierIdOrderByPeriodDesc(@Param("supplierId") Long supplierId,
			Pageable pageable);

	@Query("SELECT p FROM SupplierPerformance p WHERE p.account.accountId = :accountId AND p.overallScore >= :minScore")
	Optional<List<SupplierPerformance>> findByAccountIdAndMinScore(@Param("accountId") Long accountId,
			@Param("minScore") java.math.BigDecimal minScore);

	@Query("SELECT p FROM SupplierPerformance p WHERE p.account.accountId = :accountId AND p.overallScore <= :maxScore")
	Optional<List<SupplierPerformance>> findByAccountIdAndMaxScore(@Param("accountId") Long accountId,
			@Param("maxScore") java.math.BigDecimal maxScore);
}