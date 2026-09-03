package com.nexus.core.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.nexus.core.entities.Account;
import com.nexus.core.entities.GoodsReceipt;
import com.nexus.core.entities.GoodsReceiptLineItem;
import com.nexus.core.entities.Invoice;
import com.nexus.core.entities.PurchaseOrder;
import com.nexus.core.entities.PurchaseOrderLineItem;
import com.nexus.core.entities.Supplier;
import com.nexus.core.entities.SupplierPerformance;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.SupplierPerformanceDto;
import com.nexus.core.repository.AccountRepo;
import com.nexus.core.repository.GoodsReceiptLineItemRepo;
import com.nexus.core.repository.GoodsReceiptRepo;
import com.nexus.core.repository.InvoiceRepo;
import com.nexus.core.repository.PurchaseOrderLineItemRepo;
import com.nexus.core.repository.PurchaseOrderRepo;
import com.nexus.core.repository.SupplierPerformanceRepo;
import com.nexus.core.repository.SupplierRepository;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.SupplierPerformanceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupplierPerformanceServiceImpl implements SupplierPerformanceService {

	private final SupplierPerformanceRepo performanceRepo;
	private final SupplierRepository supplierRepository;
	private final AccountRepo accountRepo;
	private final PurchaseOrderRepo purchaseOrderRepo;
	private final PurchaseOrderLineItemRepo poLineItemRepo;
	private final GoodsReceiptRepo goodsReceiptRepo;
	private final GoodsReceiptLineItemRepo grLineItemRepo;
	private final InvoiceRepo invoiceRepo;
	private final ModelMapper modelMapper;

	@Override
	public ResponseEntity<?> createPerformanceRecord(SupplierPerformanceDto performanceDto) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		Supplier supplier = supplierRepository.findById(performanceDto.getSupplierId())
				.orElseThrow(
						() -> new ResourceNotFoundException("Supplier", "supplierId", performanceDto.getSupplierId()));

		Account account = accountRepo.findById(performanceDto.getAccountId())
				.orElseThrow(
						() -> new ResourceNotFoundException("Account", "accountId", performanceDto.getAccountId()));

		SupplierPerformance performance = modelMapper.map(performanceDto, SupplierPerformance.class);
		performance.setSupplier(supplier);
		performance.setAccount(account);
		performance.setCalculatedAt(Timestamp.valueOf(LocalDateTime.now()));
		performance.setCalculatedBy(performanceDto.getCalculatedBy());

		// Calculate overall score and tier if not provided
		if (performance.getOverallScore() == null) {
			performance.calculateOverallScore();
		}
		if (performance.getPerformanceTier() == null) {
			performance.determinePerformanceTier();
		}

		SupplierPerformance savedPerformance = performanceRepo.save(performance);
		return new ResponseEntity<>(modelMapper.map(savedPerformance, SupplierPerformanceDto.class),
				HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<?> getPerformanceById(Long id) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		SupplierPerformance performance = performanceRepo.findByPerformanceIdAndAccountAccountId(id, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("SupplierPerformance", "performanceId", id));

		return new ResponseEntity<>(modelMapper.map(performance, SupplierPerformanceDto.class), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getPerformanceBySupplier(Long supplierId, Pageable pageable) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		// Verify supplier belongs to this org
		supplierRepository.findBySupplierIdAndAccountAccountId(supplierId, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier", "supplierId", supplierId));

		Page<SupplierPerformance> performances = performanceRepo.findBySupplierSupplierIdOrderByPeriodDesc(supplierId,
				pageable);
		List<SupplierPerformanceDto> dtos = performances.stream()
				.map(p -> modelMapper.map(p, SupplierPerformanceDto.class))
				.collect(Collectors.toList());

		return new ResponseEntity<>(dtos, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getPerformanceByAccount(Long accountId, Pageable pageable) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		if (!orgId.equals(accountId)) {
			throw new SecurityException("Access denied: Cannot view performance for other organizations");
		}

		Page<SupplierPerformance> performances = performanceRepo.findByAccountAccountIdOrderByPeriodDesc(accountId, pageable);
		List<SupplierPerformanceDto> dtos = performances.stream()
				.map(p -> modelMapper.map(p, SupplierPerformanceDto.class))
				.collect(Collectors.toList());

		return new ResponseEntity<>(dtos, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getPerformanceByAccountAndPeriod(Long accountId, Date startDate, Date endDate,
			Pageable pageable) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		if (!orgId.equals(accountId)) {
			throw new SecurityException("Access denied: Cannot view performance for other organizations");
		}

		Optional<List<SupplierPerformance>> performances = performanceRepo.findByAccountAccountIdAndPeriod(accountId,
				startDate, endDate);
		List<SupplierPerformanceDto> dtos = performances.orElse(List.of()).stream()
				.map(p -> modelMapper.map(p, SupplierPerformanceDto.class))
				.collect(Collectors.toList());

		return new ResponseEntity<>(dtos, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getPerformanceBySupplierAndPeriod(Long supplierId, Date startDate, Date endDate,
			Pageable pageable) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		supplierRepository.findBySupplierIdAndAccountAccountId(supplierId, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier", "supplierId", supplierId));

		Optional<List<SupplierPerformance>> performances = performanceRepo.findBySupplierSupplierIdAndPeriod(supplierId,
				startDate, endDate);
		List<SupplierPerformanceDto> dtos = performances.orElse(List.of()).stream()
				.map(p -> modelMapper.map(p, SupplierPerformanceDto.class))
				.collect(Collectors.toList());

		return new ResponseEntity<>(dtos, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getPerformanceByAccountAndTier(Long accountId, String tier, Pageable pageable) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		if (!orgId.equals(accountId)) {
			throw new SecurityException("Access denied: Cannot view performance for other organizations");
		}

		SupplierPerformance.PerformanceTier performanceTier;
		try {
			performanceTier = SupplierPerformance.PerformanceTier.valueOf(tier.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid performance tier: " + tier);
		}

		Optional<List<SupplierPerformance>> performances = performanceRepo.findByAccountAccountIdAndTier(accountId,
				performanceTier);
		List<SupplierPerformanceDto> dtos = performances.orElse(List.of()).stream()
				.map(p -> modelMapper.map(p, SupplierPerformanceDto.class))
				.collect(Collectors.toList());

		return new ResponseEntity<>(dtos, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getLatestPerformanceBySupplier(Long supplierId) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		supplierRepository.findBySupplierIdAndAccountAccountId(supplierId, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier", "supplierId", supplierId));

		Optional<SupplierPerformance> performance = performanceRepo.findLatestBySupplierId(supplierId);
		if (performance.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		return new ResponseEntity<>(modelMapper.map(performance.get(), SupplierPerformanceDto.class), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getPerformanceSummaryByAccount(Long accountId) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		if (!orgId.equals(accountId)) {
			throw new SecurityException("Access denied: Cannot view performance for other organizations");
		}

		Optional<List<SupplierPerformance>> performances = performanceRepo.findByAccountAccountId(accountId);
		if (performances.isEmpty()) {
			return new ResponseEntity<>(Map.of("message", "No performance records found"), HttpStatus.OK);
		}

		List<SupplierPerformance> perfList = performances.get();
		Map<String, Object> summary = Map.of(
				"totalSuppliers", perfList.stream().map(p -> p.getSupplier().getSupplierId()).distinct().count(),
				"totalEvaluations", perfList.size(),
				"avgOverallScore", perfList.stream()
						.map(SupplierPerformance::getOverallScore)
						.filter(s -> s != null)
						.reduce(BigDecimal.ZERO, BigDecimal::add)
						.divide(BigDecimal.valueOf(perfList.size()), 2, RoundingMode.HALF_UP),
				"tierDistribution", perfList.stream()
						.collect(Collectors.groupingBy(p -> p.getPerformanceTier().name(), Collectors.counting())),
				"topPerformers", perfList.stream()
						.filter(p -> p.getPerformanceTier() == SupplierPerformance.PerformanceTier.EXCELLENT
								|| p.getPerformanceTier() == SupplierPerformance.PerformanceTier.GOOD)
						.count(),
				"atRiskSuppliers", perfList.stream()
						.filter(p -> p.getPerformanceTier() == SupplierPerformance.PerformanceTier.BELOW_AVERAGE
								|| p.getPerformanceTier() == SupplierPerformance.PerformanceTier.POOR)
						.count());

		return new ResponseEntity<>(summary, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getPerformanceSummaryBySupplier(Long supplierId) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		supplierRepository.findBySupplierIdAndAccountAccountId(supplierId, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier", "supplierId", supplierId));

		Optional<List<SupplierPerformance>> performances = performanceRepo.findBySupplierSupplierId(supplierId);
		if (performances.isEmpty()) {
			return new ResponseEntity<>(Map.of("message", "No performance records found for supplier"), HttpStatus.OK);
		}

		List<SupplierPerformance> perfList = performances.get();
		SupplierPerformance latest = perfList.stream()
				.max((p1, p2) -> p1.getEvaluationPeriodEnd().compareTo(p2.getEvaluationPeriodEnd()))
				.orElse(null);

		Map<String, Object> summary = Map.of(
				"supplierId", supplierId,
				"totalEvaluations", perfList.size(),
				"latestOverallScore", latest != null ? latest.getOverallScore() : null,
				"latestTier", latest != null ? latest.getPerformanceTier() : null,
				"avgOverallScore", perfList.stream()
						.map(SupplierPerformance::getOverallScore)
						.filter(s -> s != null)
						.reduce(BigDecimal.ZERO, BigDecimal::add)
						.divide(BigDecimal.valueOf(perfList.size()), 2, RoundingMode.HALF_UP),
				"avgOtifScore", perfList.stream()
						.map(SupplierPerformance::getOtifScore)
						.filter(s -> s != null)
						.reduce(BigDecimal.ZERO, BigDecimal::add)
						.divide(BigDecimal.valueOf(perfList.size()), 2, RoundingMode.HALF_UP),
				"avgQualityDefectRate", perfList.stream()
						.map(SupplierPerformance::getQualityDefectRate)
						.filter(s -> s != null)
						.reduce(BigDecimal.ZERO, BigDecimal::add)
						.divide(BigDecimal.valueOf(perfList.size()), 2, RoundingMode.HALF_UP),
				"avgLeadTimeDays", perfList.stream()
						.map(SupplierPerformance::getAvgLeadTimeDays)
						.filter(s -> s != null)
						.mapToInt(Integer::intValue)
						.average()
						.orElse(0.0),
				"avgResponsivenessScore", perfList.stream()
						.map(SupplierPerformance::getResponsivenessScore)
						.filter(s -> s != null)
						.reduce(BigDecimal.ZERO, BigDecimal::add)
						.divide(BigDecimal.valueOf(perfList.size()), 2, RoundingMode.HALF_UP),
				"trend", calculateTrend(perfList));

		return new ResponseEntity<>(summary, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> calculateAndSavePerformance(Long supplierId, Date startDate, Date endDate,
			String calculatedBy) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		Supplier supplier = supplierRepository.findBySupplierIdAndAccountAccountId(supplierId, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier", "supplierId", supplierId));

		// Get all POs for this supplier in the period
		List<PurchaseOrder> purchaseOrders = purchaseOrderRepo.findBySupplierSupplierIdAndBuyerOrgAccountId(supplierId, orgId)
				.orElse(List.of()).stream()
				.filter(po -> po.getRequestedDeliveryDate() != null
						&& !po.getRequestedDeliveryDate().before(startDate)
						&& !po.getRequestedDeliveryDate().after(endDate))
				.collect(Collectors.toList());

		if (purchaseOrders.isEmpty()) {
			return new ResponseEntity<>(Map.of("message", "No purchase orders found for supplier in period"),
					HttpStatus.OK);
		}

		// Calculate metrics
		int totalOrders = purchaseOrders.size();
		int onTimeDeliveries = 0;
		int inFullDeliveries = 0;
		int totalDefects = 0;
		int totalUnitsReceived = 0;
		long totalLeadTimeDays = 0;
		int leadTimeCount = 0;
		long totalResponseTimeHours = 0;
		int responseTimeCount = 0;

		for (PurchaseOrder po : purchaseOrders) {
			// Get Goods Receipts for this PO
			List<GoodsReceipt> goodsReceipts = goodsReceiptRepo
					.findByPurchaseOrderPurchaseOrderId(po.getPurchaseOrderId())
					.orElse(List.of());

			for (GoodsReceipt gr : goodsReceipts) {
				if (gr.getStatus() == com.nexus.core.entities.GoodsReceiptStatus.RECEIVED) {
					// Check on-time delivery
					if (gr.getReceivedDate() != null && po.getRequestedDeliveryDate() != null
							&& !gr.getReceivedDate().after(po.getRequestedDeliveryDate())) {
						onTimeDeliveries++;
					}

					// Calculate lead time
					if (po.getRequestedDeliveryDate() != null && gr.getReceivedDate() != null) {
						long diff = gr.getReceivedDate().getTime() - po.getRequestedDeliveryDate().getTime();
						totalLeadTimeDays += diff / (1000 * 60 * 60 * 24);
						leadTimeCount++;
					}

					// Check in-full delivery
					for (GoodsReceiptLineItem grLine : gr.getLineItems()) {
						PurchaseOrderLineItem poLine = poLineItemRepo
								.findByLineItemId(grLine.getPoLineItem().getLineItemId())
								.orElse(null);
						if (poLine != null) {
							totalUnitsReceived += grLine.getQuantityReceived().intValue();
							BigDecimal poQuantityOrdered = BigDecimal.valueOf(poLine.getQuantityOrdered());
							if (grLine.getQuantityReceived().compareTo(poQuantityOrdered) >= 0) {
								inFullDeliveries++;
							}
						}
					}
				}
			}

			// Get Invoices for defect tracking (simplified - using invoice rejections as
			// proxy)
			List<Invoice> invoices = invoiceRepo.findByPurchaseOrderPurchaseOrderId(po.getPurchaseOrderId())
					.orElse(List.of());
			for (Invoice inv : invoices) {
				if (inv.getStatus() == com.nexus.core.entities.InvoiceStatus.REJECTED) {
					totalDefects++;
				}
			}
		}

		// Calculate OTIF
		BigDecimal otifScore = BigDecimal.ZERO;
		if (totalOrders > 0) {
			otifScore = BigDecimal.valueOf(onTimeDeliveries)
					.divide(BigDecimal.valueOf(totalOrders), 4, RoundingMode.HALF_UP)
					.multiply(BigDecimal.valueOf(100))
					.setScale(2, RoundingMode.HALF_UP);
		}

		// Calculate quality defect rate
		BigDecimal qualityDefectRate = BigDecimal.ZERO;
		if (totalUnitsReceived > 0) {
			qualityDefectRate = BigDecimal.valueOf(totalDefects)
					.divide(BigDecimal.valueOf(totalUnitsReceived), 4, RoundingMode.HALF_UP)
					.multiply(BigDecimal.valueOf(100))
					.setScale(2, RoundingMode.HALF_UP);
		}

		// Calculate average lead time
		Integer avgLeadTimeDays = leadTimeCount > 0 ? (int) (totalLeadTimeDays / leadTimeCount) : 0;

		// Calculate responsiveness (simplified - using invoice approval time as proxy)
		Integer avgResponseTimeHours = responseTimeCount > 0 ? (int) (totalResponseTimeHours / responseTimeCount) : 0;
		BigDecimal responsivenessScore = BigDecimal.valueOf(100); // Default to 100 if no data

		// Create performance record
		SupplierPerformanceDto performanceDto = new SupplierPerformanceDto();
		performanceDto.setSupplierId(supplierId);
		performanceDto.setAccountId(orgId);
		performanceDto.setEvaluationPeriodStart(startDate);
		performanceDto.setEvaluationPeriodEnd(endDate);
		performanceDto.setOtifScore(otifScore);
		performanceDto.setQualityDefectRate(qualityDefectRate);
		performanceDto.setAvgLeadTimeDays(avgLeadTimeDays);
		performanceDto.setResponsivenessScore(responsivenessScore);
		performanceDto.setTotalOrdersEvaluated(totalOrders);
		performanceDto.setOnTimeDeliveries(onTimeDeliveries);
		performanceDto.setInFullDeliveries(inFullDeliveries);
		performanceDto.setTotalDefects(totalDefects);
		performanceDto.setTotalUnitsReceived(totalUnitsReceived);
		performanceDto.setAvgResponseTimeHours(avgResponseTimeHours);
		performanceDto.setCalculatedBy(calculatedBy);

		return createPerformanceRecord(performanceDto);
	}

	@Override
	public ResponseEntity<?> updatePerformanceRecord(Long id, SupplierPerformanceDto performanceDto) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		SupplierPerformance performance = performanceRepo.findByPerformanceIdAndAccountAccountId(id, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("SupplierPerformance", "performanceId", id));

		// Update fields
		if (performanceDto.getOtifScore() != null)
			performance.setOtifScore(performanceDto.getOtifScore());
		if (performanceDto.getQualityDefectRate() != null)
			performance.setQualityDefectRate(performanceDto.getQualityDefectRate());
		if (performanceDto.getAvgLeadTimeDays() != null)
			performance.setAvgLeadTimeDays(performanceDto.getAvgLeadTimeDays());
		if (performanceDto.getResponsivenessScore() != null)
			performance.setResponsivenessScore(performanceDto.getResponsivenessScore());
		if (performanceDto.getTotalOrdersEvaluated() != null)
			performance.setTotalOrdersEvaluated(performanceDto.getTotalOrdersEvaluated());
		if (performanceDto.getOnTimeDeliveries() != null)
			performance.setOnTimeDeliveries(performanceDto.getOnTimeDeliveries());
		if (performanceDto.getInFullDeliveries() != null)
			performance.setInFullDeliveries(performanceDto.getInFullDeliveries());
		if (performanceDto.getTotalDefects() != null)
			performance.setTotalDefects(performanceDto.getTotalDefects());
		if (performanceDto.getTotalUnitsReceived() != null)
			performance.setTotalUnitsReceived(performanceDto.getTotalUnitsReceived());
		if (performanceDto.getAvgResponseTimeHours() != null)
			performance.setAvgResponseTimeHours(performanceDto.getAvgResponseTimeHours());

		// Recalculate overall score and tier
		performance.calculateOverallScore();
		performance.determinePerformanceTier();

		SupplierPerformance updated = performanceRepo.save(performance);
		return new ResponseEntity<>(modelMapper.map(updated, SupplierPerformanceDto.class), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> deletePerformanceRecord(Long id) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		SupplierPerformance performance = performanceRepo.findByPerformanceIdAndAccountAccountId(id, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("SupplierPerformance", "performanceId", id));

		performanceRepo.delete(performance);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	private String calculateTrend(List<SupplierPerformance> performances) {
		if (performances.size() < 2) {
			return "INSUFFICIENT_DATA";
		}

		List<SupplierPerformance> sorted = performances.stream()
				.sorted((p1, p2) -> p1.getEvaluationPeriodEnd().compareTo(p2.getEvaluationPeriodEnd()))
				.collect(Collectors.toList());

		BigDecimal first = sorted.get(0).getOverallScore();
		BigDecimal last = sorted.get(sorted.size() - 1).getOverallScore();

		if (first == null || last == null) {
			return "INSUFFICIENT_DATA";
		}

		BigDecimal diff = last.subtract(first);
		if (diff.compareTo(BigDecimal.valueOf(5)) > 0) {
			return "IMPROVING";
		} else if (diff.compareTo(BigDecimal.valueOf(-5)) < 0) {
			return "DECLINING";
		} else {
			return "STABLE";
		}
	}
}