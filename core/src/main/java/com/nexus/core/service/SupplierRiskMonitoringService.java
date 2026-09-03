package com.nexus.core.service;

import com.nexus.core.dto.SupplierRiskMonitoringDTO;
import com.nexus.core.dto.SupplierRiskMonitoringCreateRequest;
import com.nexus.core.dto.SupplierRiskMonitoringUpdateRequest;
import com.nexus.core.dto.SupplierRiskSummaryDTO;

import java.util.List;

/**
 * Service interface for Supplier Risk Monitoring operations.
 * FR-RET-024: Supplier Risk Monitoring
 */
public interface SupplierRiskMonitoringService {

	SupplierRiskMonitoringDTO createRiskMonitoring(SupplierRiskMonitoringCreateRequest request, Long orgId);

	SupplierRiskMonitoringDTO getRiskMonitoringById(Long riskMonitoringId, Long orgId);

	List<SupplierRiskMonitoringDTO> getRiskMonitoringBySupplierId(Long supplierId, Long orgId);

	List<SupplierRiskMonitoringDTO> getRiskMonitoringByPartnershipId(Long partnershipId, Long orgId);

	List<SupplierRiskMonitoringDTO> getRiskMonitoringByRiskLevel(String riskLevel, Long orgId);

	List<SupplierRiskMonitoringDTO> getRiskMonitoringDueForReview(Long orgId);

	SupplierRiskMonitoringDTO updateRiskMonitoring(Long riskMonitoringId, SupplierRiskMonitoringUpdateRequest request,
			Long orgId);

	void deleteRiskMonitoring(Long riskMonitoringId, Long orgId);

	SupplierRiskSummaryDTO getSupplierRiskSummary(Long supplierId, Long orgId);

	List<SupplierRiskMonitoringDTO> getRiskMonitoringByCategory(Long supplierId, String riskCategory, Long orgId);

	List<SupplierRiskMonitoringDTO> getAllRiskMonitoring(Long orgId, int pageNo, int pageSize);
}