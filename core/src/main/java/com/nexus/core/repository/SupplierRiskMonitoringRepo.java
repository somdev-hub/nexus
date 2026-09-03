package com.nexus.core.repository;

import com.nexus.core.entities.SupplierRiskMonitoring;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for SupplierRiskMonitoring entity.
 * FR-RET-024: Supplier Risk Monitoring
 */
@Repository
public interface SupplierRiskMonitoringRepo extends JpaRepository<SupplierRiskMonitoring, Long> {

	Page<SupplierRiskMonitoring> findBySupplierSupplierIdAndIsActiveTrue(Long supplierId, Pageable pageable);

	List<SupplierRiskMonitoring> findBySupplierSupplierIdAndIsActiveTrue(Long supplierId);

	@Query("SELECT srm FROM SupplierRiskMonitoring srm WHERE srm.supplier.supplierId = :supplierId AND srm.riskLevel IN (:riskLevels) AND srm.isActive = true")
	List<SupplierRiskMonitoring> findBySupplierIdAndRiskLevels(@Param("supplierId") Long supplierId,
			@Param("riskLevels") List<String> riskLevels);

	@Query("SELECT srm FROM SupplierRiskMonitoring srm WHERE srm.partnership.partnershipId = :partnershipId AND srm.isActive = true")
	List<SupplierRiskMonitoring> findByPartnershipId(@Param("partnershipId") Long partnershipId);

	@Query("SELECT srm FROM SupplierRiskMonitoring srm WHERE srm.riskLevel = :riskLevel AND srm.isActive = true")
	List<SupplierRiskMonitoring> findByRiskLevel(@Param("riskLevel") String riskLevel);

	@Query("SELECT srm FROM SupplierRiskMonitoring srm WHERE srm.nextReviewDate <= :date AND srm.isActive = true AND srm.status IN ('OPEN', 'IN_PROGRESS')")
	List<SupplierRiskMonitoring> findDueForReview(@Param("date") LocalDate date);

	@Query("SELECT srm FROM SupplierRiskMonitoring srm WHERE srm.status = :status AND srm.isActive = true")
	List<SupplierRiskMonitoring> findByStatus(@Param("status") String status);

	@Query("SELECT srm FROM SupplierRiskMonitoring srm WHERE srm.supplier.supplierId = :supplierId AND srm.riskCategory = :riskCategory AND srm.isActive = true ORDER BY srm.identifiedDate DESC")
	List<SupplierRiskMonitoring> findBySupplierIdAndRiskCategory(@Param("supplierId") Long supplierId,
			@Param("riskCategory") String riskCategory);

	Optional<SupplierRiskMonitoring> findByIdAndSupplierSupplierId(Long riskMonitoringId, Long supplierId);

	boolean existsBySupplierSupplierIdAndRiskCategoryAndStatusIn(Long supplierId, String riskCategory,
			List<String> statuses);

	@Query("SELECT COUNT(srm) FROM SupplierRiskMonitoring srm WHERE srm.supplier.supplierId = :supplierId AND srm.isActive = true")
	Long countActiveBySupplierId(@Param("supplierId") Long supplierId);

	@Query("SELECT AVG(srm.riskScore) FROM SupplierRiskMonitoring srm WHERE srm.supplier.supplierId = :supplierId AND srm.isActive = true AND srm.riskScore IS NOT NULL")
	Double getAverageRiskScoreBySupplierId(@Param("supplierId") Long supplierId);
}