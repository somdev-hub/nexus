package com.nexus.core.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.SupplierPerformanceDto;

public interface SupplierPerformanceService {

	ResponseEntity<?> createPerformanceRecord(SupplierPerformanceDto performanceDto);

	ResponseEntity<?> getPerformanceById(Long id);

	ResponseEntity<?> getPerformanceBySupplier(Long supplierId, Pageable pageable);

	ResponseEntity<?> getPerformanceByAccount(Long accountId, Pageable pageable);

	ResponseEntity<?> getPerformanceByAccountAndPeriod(Long accountId, Date startDate, Date endDate, Pageable pageable);

	ResponseEntity<?> getPerformanceBySupplierAndPeriod(Long supplierId, Date startDate, Date endDate,
			Pageable pageable);

	ResponseEntity<?> getPerformanceByAccountAndTier(Long accountId, String tier, Pageable pageable);

	ResponseEntity<?> getLatestPerformanceBySupplier(Long supplierId);

	ResponseEntity<?> getPerformanceSummaryByAccount(Long accountId);

	ResponseEntity<?> getPerformanceSummaryBySupplier(Long supplierId);

	ResponseEntity<?> calculateAndSavePerformance(Long supplierId, Date startDate, Date endDate, String calculatedBy);

	ResponseEntity<?> updatePerformanceRecord(Long id, SupplierPerformanceDto performanceDto);

	ResponseEntity<?> deletePerformanceRecord(Long id);
}