package com.nexus.core.service.implementations;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.model.entities.CarrierPayable;
import com.nexus.core.model.entities.FreightInvoice;
import com.nexus.core.model.entities.Invoice;
import com.nexus.core.model.entities.LogisticsPayableLine;
import com.nexus.core.model.entities.Partnership;
import com.nexus.core.model.entities.PurchaseOrder;
import com.nexus.core.model.entities.Shipment;
import com.nexus.core.model.enums.FreightInvoiceStatus;
import com.nexus.core.model.enums.InvoiceStatus;
import com.nexus.core.model.enums.LogisticsPayableLineKind;
import com.nexus.core.model.enums.LogisticsPayableLineStatus;
import com.nexus.core.model.enums.PayableStatus;
import com.nexus.core.model.enums.PurchaseOrderStatus;
import com.nexus.core.model.enums.ShipmentStatus;
import com.nexus.core.repository.CarrierPayableRepo;
import com.nexus.core.repository.FreightInvoiceRepo;
import com.nexus.core.repository.InvoiceRepo;
import com.nexus.core.repository.LogisticsPayableLineRepo;
import com.nexus.core.repository.PartnershipRepo;
import com.nexus.core.repository.PurchaseOrderRepo;
import com.nexus.core.repository.ShipmentRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.interfaces.FinanceService;
import com.nexus.core.service.interfaces.LogisticsBillingService;

import lombok.RequiredArgsConstructor;

/**
 * Cross-entity finance analytics. Outstanding = APPROVED but not PAID;
 * overdue adds dueDate < today. All sums skip CANCELLED/REJECTED rows.
 */
@Service
@RequiredArgsConstructor
public class FinanceServiceImpl implements FinanceService {

	private final PurchaseOrderRepo poRepo;
	private final InvoiceRepo invoiceRepo;
	private final ShipmentRepo shipmentRepo;
	private final FreightInvoiceRepo freightRepo;
	private final CarrierPayableRepo payableRepo;
	private final PartnershipRepo partnershipRepo;
	private final LogisticsPayableLineRepo lineRepo;
	private final LogisticsBillingService billingService;

	// ── retailer ──────────────────────────────────────────────
	@Override
	@Transactional(readOnly = true)
	@LogActivity("Retailer Finance Summary")
	public ResponseEntity<?> retailerSummary(String period) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Timestamp since = since(period);
		List<PurchaseOrder> pos = poRepo.findByBuyerOrgAccountId(orgId, Pageable.unpaged()).getContent()
				.stream().filter(p -> p.getStatus() != PurchaseOrderStatus.CANCELLED
						&& p.getStatus() != PurchaseOrderStatus.REJECTED)
				.toList();
		List<Invoice> invoices = invoiceRepo.findByPurchaseOrderBuyerOrgId(orgId, Pageable.unpaged())
				.getContent().stream().filter(i -> i.getStatus() != InvoiceStatus.CANCELLED
						&& i.getStatus() != InvoiceStatus.REJECTED)
				.toList();

		BigDecimal spend = sumPo(pos);
		BigDecimal paid = sumInv(invoices, InvoiceStatus.PAID);
		BigDecimal outstanding = sumInv(invoices, InvoiceStatus.APPROVED);
		BigDecimal overdue = sumOverdue(invoices, InvoiceStatus.APPROVED);

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("totalSpend", spend);
		res.put("paid", paid);
		res.put("outstanding", outstanding);
		res.put("overdue", overdue);
		res.put("openPoCount", pos.stream().filter(p -> p.getStatus() != PurchaseOrderStatus.PAID
				&& p.getStatus() != PurchaseOrderStatus.CLOSED).count());
		res.put("aging", aging(invoices, InvoiceStatus.APPROVED));
		res.put("trend", monthlyTrend(invoices, since));
		res.put("topSuppliers", topSuppliers(invoices));
		res.put("funnel", funnel(pos, invoices));
		return ResponseEntity.ok(res);
	}

	@Override
	@Transactional(readOnly = true)
	@LogActivity("Retailer Finance Transactions")
	public ResponseEntity<?> retailerTransactions(String kind, String status, String search,
			Date from, Date to, Pageable pageable) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		List<Map<String, Object>> rows = new ArrayList<>();
		if (kind == null || "INVOICE".equalsIgnoreCase(kind)) {
			for (Invoice i : invoiceRepo.findByPurchaseOrderBuyerOrgId(orgId, Pageable.unpaged()).getContent()) {
				if (!matchStatus(status, i.getStatus().name()))
					continue;
				if (!inRange(i.getInvoiceDate(), from, to))
					continue;
				String num = i.getSupplierInvoiceNumber() != null ? i.getSupplierInvoiceNumber()
						: i.getInvoiceNumber();
				String party = i.getPurchaseOrder() != null && i.getPurchaseOrder().getSupplier() != null
						? i.getPurchaseOrder().getSupplier().getBusinessName()
						: null;
				if (!matchSearch(search, num, party))
					continue;
				Map<String, Object> r = new LinkedHashMap<>();
				r.put("kind", "INVOICE_PAYABLE");
				r.put("id", i.getInvoiceId());
				r.put("number", num);
				r.put("party", party);
				r.put("amount", i.getTotalAmount());
				r.put("currency", i.getCurrency());
				r.put("status", i.getStatus().name());
				r.put("date", i.getInvoiceDate());
				r.put("dueDate", i.getDueDate());
				r.put("paidAt", i.getPaidAt());
				rows.add(r);
			}
		}
		if (kind == null || "FREIGHT".equalsIgnoreCase(kind)) {
			for (FreightInvoice f : freightRepo.findByOrgWithFilters(orgId, null, null, null, null, null, null,
					null, null, Pageable.unpaged()).getContent()) {
				if (!matchStatus(status, f.getStatus().name()))
					continue;
				java.sql.Date issued = f.getIssuedDate() != null ? new Date(f.getIssuedDate().getTime()) : null;
				if (!inRange(issued, from, to))
					continue;
				if (!matchSearch(search, f.getInvoiceNumber(), null))
					continue;
				Map<String, Object> r = new LinkedHashMap<>();
				r.put("kind", "FREIGHT");
				r.put("id", f.getFreightInvoiceId());
				r.put("number", f.getInvoiceNumber());
				r.put("party", null);
				r.put("amount", f.getTotalAmount());
				r.put("currency", f.getCurrency());
				r.put("status", f.getStatus().name());
				r.put("date", f.getIssuedDate());
				r.put("dueDate", f.getDueDate());
				r.put("paidAt", f.getPaidDate());
				rows.add(r);
			}
		}
		return page(rows, pageable);
	}

	// ── supplier ──────────────────────────────────────────────
	@Override
	@Transactional(readOnly = true)
	@LogActivity("Supplier Finance Summary")
	public ResponseEntity<?> supplierSummary(String period) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Timestamp since = since(period);
		List<PurchaseOrder> orders = poRepo.findSupplierVisibleOrdersList(orgId).stream()
				.filter(p -> p.getStatus() != PurchaseOrderStatus.CANCELLED
						&& p.getStatus() != PurchaseOrderStatus.REJECTED)
				.toList();
		List<Invoice> invoices = new ArrayList<>();
		for (PurchaseOrder o : orders) {
			invoiceRepo.findByPurchaseOrderPurchaseOrderId(o.getPurchaseOrderId()).ifPresent(invoices::addAll);
		}
		invoices = invoices.stream().filter(i -> i.getStatus() != InvoiceStatus.CANCELLED
				&& i.getStatus() != InvoiceStatus.REJECTED).toList();

		BigDecimal sales = sumPo(orders);
		BigDecimal received = sumInv(invoices, InvoiceStatus.PAID);
		BigDecimal outstanding = sumInv(invoices, InvoiceStatus.APPROVED);
		BigDecimal overdue = sumOverdue(invoices, InvoiceStatus.APPROVED);

		// Logistics payables for this supplier: delivered trips + open
		// settlement lines + freight bills (fallback chain per shipment).
		BigDecimal logiOpen = BigDecimal.ZERO, logiOverdue = BigDecimal.ZERO;
		List<Shipment> supplierDelivered = shipmentRepo.findBySupplierOrgAccountId(orgId, Pageable.unpaged())
				.getContent().stream()
				.filter(s -> s.getStatus() == ShipmentStatus.DELIVERED
						|| s.getStatus() == ShipmentStatus.PARTIALLY_DELIVERED)
				.toList();
		Map<String, BigDecimal> supplierRecv = settlementReceivable(supplierDelivered);
		logiOpen = supplierRecv.get("open");
		logiOverdue = supplierRecv.get("overdue");

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("totalSales", sales);
		res.put("received", received);
		res.put("outstanding", outstanding);
		res.put("overdue", overdue);
		res.put("logisticsOpen", logiOpen);
		res.put("logisticsOverdue", logiOverdue);
		// Currency the settlement KPIs are denominated in (lane currency),
		// separate from the invoice-side USD aggregates above.
		res.put("logisticsCurrency", settlementCurrency(supplierDelivered));
		res.put("openOrderCount", orders.stream().filter(o -> o.getStatus() != PurchaseOrderStatus.PAID
				&& o.getStatus() != PurchaseOrderStatus.CLOSED).count());
		res.put("aging", aging(invoices, InvoiceStatus.APPROVED));
		res.put("trend", monthlyTrend(invoices, since));
		res.put("topBuyers", topBuyers(orders));
		return ResponseEntity.ok(res);
	}

	@Override
	@Transactional(readOnly = true)
	@LogActivity("Supplier Finance Transactions")
	public ResponseEntity<?> supplierTransactions(String kind, String status, String search,
			Date from, Date to, Pageable pageable) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		List<Map<String, Object>> rows = new ArrayList<>();
		if (kind == null || "ORDER".equalsIgnoreCase(kind) || "INVOICE".equalsIgnoreCase(kind)) {
			for (PurchaseOrder o : poRepo.findSupplierVisibleOrdersList(orgId)) {
				String buyer = o.getBuyerOrg() != null ? String.valueOf(o.getBuyerOrg().getAccountId()) : null;
				if (kind == null || "ORDER".equalsIgnoreCase(kind)) {
					if (!matchStatus(status, o.getStatus().name()))
						continue;
					if (!matchSearch(search, o.getPoNumber(), buyer))
						continue;
					Map<String, Object> r = new LinkedHashMap<>();
					r.put("kind", "ORDER");
					r.put("id", o.getPurchaseOrderId());
					r.put("number", o.getPoNumber());
					r.put("party", buyer);
					r.put("amount", o.getTotalAmount());
					r.put("currency", o.getCurrency());
					r.put("status", o.getStatus().name());
					r.put("date", o.getCreatedAt());
					r.put("dueDate", o.getExpectedDeliveryDate());
					r.put("paidAt", null);
					rows.add(r);
				}
				if (kind == null || "INVOICE".equalsIgnoreCase(kind)) {
					for (Invoice i : invoiceRepo.findByPurchaseOrderPurchaseOrderId(o.getPurchaseOrderId())
							.orElse(List.of())) {
						if (!matchStatus(status, i.getStatus().name()))
							continue;
						if (!inRange(i.getInvoiceDate(), from, to))
							continue;
						String num = i.getSupplierInvoiceNumber() != null ? i.getSupplierInvoiceNumber()
								: i.getInvoiceNumber();
						if (!matchSearch(search, num, buyer))
							continue;
						Map<String, Object> r = new LinkedHashMap<>();
						r.put("kind", "INVOICE_RECEIVABLE");
						r.put("id", i.getInvoiceId());
						r.put("number", num);
						r.put("party", buyer);
						r.put("amount", i.getTotalAmount());
						r.put("currency", i.getCurrency());
						r.put("status", i.getStatus().name());
						r.put("date", i.getInvoiceDate());
						r.put("dueDate", i.getDueDate());
						r.put("paidAt", i.getPaidAt());
						rows.add(r);
					}
				}
			}
		}
		if (kind == null || "LOGISTICS".equalsIgnoreCase(kind)) {
			for (Partnership p : logisticsPartnershipsOf(orgId)) {
				for (LogisticsPayableLine l : lineRepo.findByPartnershipId(p.getPartnershipId())) {
					if (!matchStatus(status, l.getStatus().name()))
						continue;
					Date d = l.getDueDate() != null ? new Date(l.getDueDate().getTime()) : null;
					if (!inRange(d, from, to))
						continue;
					if (!matchSearch(search, String.valueOf(l.getShipmentId()),
							String.valueOf(p.getPartnershipId())))
						continue;
					Map<String, Object> r = new LinkedHashMap<>();
					r.put("kind", "LOGISTICS_" + l.getKind().name());
					r.put("id", l.getLineId());
					r.put("number", l.getCycleId() != null ? l.getCycleId() : ("TRIP-" + l.getShipmentId()));
					r.put("party", "partnership-" + p.getPartnershipId());
					r.put("amount", l.getAmount());
					r.put("currency", l.getCurrency());
					r.put("status", l.getStatus().name());
					r.put("date", l.getCreatedAt());
					r.put("dueDate", l.getDueDate());
					r.put("paidAt", null);
					r.put("partnershipId", p.getPartnershipId());
					rows.add(r);
				}
			}
		}
		return page(rows, pageable);
	}

	// ── logistics ─────────────────────────────────────────────
	@Override
	@Transactional(readOnly = true)
	@LogActivity("Logistics Finance Summary")
	public ResponseEntity<?> logisticsSummary(String period) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Timestamp since = since(period);
		List<Shipment> ships = shipmentRepo.findByLogisticsOrgAccountId(orgId, Pageable.unpaged())
				.getContent();
		BigDecimal revenue = BigDecimal.ZERO;
		Map<String, BigDecimal> perLane = new LinkedHashMap<>();
		Map<String, BigDecimal> perCustomer = new LinkedHashMap<>();
		long delivered = 0;
		for (Shipment s : ships) {
			if (s.getStatus() == ShipmentStatus.CANCELLED)
				continue;
			BigDecimal fare = billingService.tripFare(s);
			if (s.getStatus() == ShipmentStatus.DELIVERED
					|| s.getStatus() == ShipmentStatus.PARTIALLY_DELIVERED) {
				delivered++;
				revenue = revenue.add(fare);
				String lane = s.getPickupLocation() + " → " + s.getDeliveryLocation();
				perLane.merge(lane, fare, BigDecimal::add);
				String cust = s.getRetailerOrg() != null ? String.valueOf(s.getRetailerOrg().getAccountId())
						: "unknown";
				perCustomer.merge(cust, fare, BigDecimal::add);
			}
		}
		long openPayables = payableRepo.findByOrgWithFilters(orgId, null, PayableStatus.PENDING, null,
				Pageable.unpaged()).getTotalElements();
		long disputes = payableRepo.findByOrgWithFilters(orgId, null, PayableStatus.DISPUTED, null,
				Pageable.unpaged()).getTotalElements();

		BigDecimal recvOpen = BigDecimal.ZERO, recvOverdue = BigDecimal.ZERO, penalties = BigDecimal.ZERO;
		List<Shipment> deliveredShips = ships.stream()
				.filter(s -> s.getStatus() == ShipmentStatus.DELIVERED
						|| s.getStatus() == ShipmentStatus.PARTIALLY_DELIVERED)
				.toList();
		Map<String, BigDecimal> recv = settlementReceivable(deliveredShips);
		recvOpen = recv.get("open");
		recvOverdue = recv.get("overdue");
		penalties = recv.get("penalties");

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("totalRevenue", revenue);
		res.put("deliveredShipments", delivered);
		res.put("totalShipments", ships.size());
		res.put("revenuePerLane", perLane);
		res.put("revenuePerCustomer", perCustomer);
		res.put("openPayables", openPayables);
		res.put("disputes", disputes);
		res.put("receivableOpen", recvOpen);
		res.put("receivableOverdue", recvOverdue);
		res.put("penaltiesLevied", penalties);
		res.put("trend", shipmentTrend(ships, since));
		// Trip revenue/receivables carry each shipment's fare currency (the
		// route capacity's when capacity-priced); expose the dominant one.
		res.put("currency", settlementCurrency(deliveredShips));
		return ResponseEntity.ok(res);
	}

	@Override
	@Transactional(readOnly = true)
	@LogActivity("Logistics Finance Transactions")
	public ResponseEntity<?> logisticsTransactions(String kind, String status, String search,
			Date from, Date to, Pageable pageable) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		List<Map<String, Object>> rows = new ArrayList<>();
		if (kind == null || "FREIGHT".equalsIgnoreCase(kind)) {
			for (FreightInvoice f : freightRepo.findByLogisticsOrgAccountId(orgId, Pageable.unpaged())
					.getContent()) {
				if (!matchStatus(status, f.getStatus().name()))
					continue;
				Date issued = f.getIssuedDate() != null ? new Date(f.getIssuedDate().getTime()) : null;
				if (!inRange(issued, from, to))
					continue;
				if (!matchSearch(search, f.getInvoiceNumber(), null))
					continue;
				Map<String, Object> r = new LinkedHashMap<>();
				r.put("kind", "FREIGHT");
				r.put("id", f.getFreightInvoiceId());
				r.put("number", f.getInvoiceNumber());
				r.put("party", null);
				r.put("amount", f.getTotalAmount());
				r.put("currency", f.getCurrency());
				r.put("status", f.getStatus().name());
				r.put("date", f.getIssuedDate());
				r.put("dueDate", f.getDueDate());
				r.put("paidAt", f.getPaidDate());
				rows.add(r);
			}
		}
		if (kind == null || "PAYABLE".equalsIgnoreCase(kind)) {
			for (CarrierPayable p : payableRepo.findByOrgWithFilters(orgId, null, null, search,
					Pageable.unpaged()).getContent()) {
				if (!matchStatus(status, p.getStatus().name()))
					continue;
				Map<String, Object> r = new LinkedHashMap<>();
				r.put("kind", "CARRIER_PAYABLE");
				r.put("id", p.getPayableId());
				r.put("number", p.getPayableNumber());
				r.put("party", p.getCarrierName());
				r.put("amount", p.getPayableAmount());
				r.put("currency", p.getCurrency());
				r.put("status", p.getStatus().name());
				r.put("date", p.getCreatedAt());
				r.put("dueDate", p.getDueDate());
				r.put("paidAt", null);
				rows.add(r);
			}
		}
		if (kind == null || "STATEMENT".equalsIgnoreCase(kind) || "LOGISTICS".equalsIgnoreCase(kind)) {
			for (Partnership p : logisticsPartnershipsOf(orgId)) {
				for (LogisticsPayableLine l : lineRepo.findByPartnershipId(p.getPartnershipId())) {
					if (!matchStatus(status, l.getStatus().name()))
						continue;
					Date d = l.getDueDate() != null ? new Date(l.getDueDate().getTime()) : null;
					if (!inRange(d, from, to))
						continue;
					if (!matchSearch(search, String.valueOf(l.getShipmentId()), l.getCycleId()))
						continue;
					Map<String, Object> r = new LinkedHashMap<>();
					r.put("kind", "SETTLEMENT_" + l.getKind().name());
					r.put("id", l.getLineId());
					r.put("number", l.getCycleId() != null ? l.getCycleId() : ("TRIP-" + l.getShipmentId()));
					r.put("party", "partnership-" + p.getPartnershipId());
					r.put("amount", l.getAmount());
					r.put("currency", l.getCurrency());
					r.put("status", l.getStatus().name());
					r.put("date", l.getCreatedAt());
					r.put("dueDate", l.getDueDate());
					r.put("paidAt", null);
					r.put("partnershipId", p.getPartnershipId());
					rows.add(r);
				}
			}
		}
		return page(rows, pageable);
	}

	// ── helpers ───────────────────────────────────────────────
	/**
	 * Settlement receivable across delivered shipments with a no-double-count
	 * fallback chain per shipment: recorded payable-line remainders first,
	 * else open freight-invoice totals, else the trip fare itself. Guarantees
	 * a non-zero figure whenever delivered work exists, even if the billing
	 * endpoints (which lazily persist lines) were never called.
	 */
	private Map<String, BigDecimal> settlementReceivable(List<Shipment> delivered) {
		BigDecimal open = BigDecimal.ZERO, overdue = BigDecimal.ZERO, penalties = BigDecimal.ZERO;
		Timestamp now = new Timestamp(System.currentTimeMillis());
		for (Shipment s : delivered) {
			List<LogisticsPayableLine> lines = lineRepo.findByShipmentId(s.getShipmentId());
			BigDecimal fare = billingService.tripFare(s);
			BigDecimal shipOpen = BigDecimal.ZERO, shipOverdue = BigDecimal.ZERO;
			if (!lines.isEmpty()) {
				for (LogisticsPayableLine l : lines) {
					if (l.getStatus() == LogisticsPayableLineStatus.PAID)
						continue;
					BigDecimal rem = l.getAmount().subtract(orZero(l.getPaidAmount()));
					if (rem.signum() <= 0)
						continue;
					shipOpen = shipOpen.add(rem);
					if (l.getKind() == LogisticsPayableLineKind.CYCLE_PENALTY)
						penalties = penalties.add(rem);
					if (l.getDueDate() != null && l.getDueDate().before(now))
						shipOverdue = shipOverdue.add(rem);
				}
				// Legacy zero-priced base lines (created before capacity
				// pricing) fall back to the resolved fare until re-priced.
				boolean unpricedBase = lines.stream().anyMatch(l ->
						l.getKind() == LogisticsPayableLineKind.BASE_TRIP
								&& orZero(l.getAmount()).signum() <= 0
								&& orZero(l.getPaidAmount()).signum() == 0
								&& l.getStatus() != LogisticsPayableLineStatus.PAID);
				if (unpricedBase && fare.signum() > 0) {
					shipOpen = shipOpen.add(fare);
					long deliveredAt = deliveredAtOf(s);
					if (System.currentTimeMillis() - deliveredAt > 7L * 86_400_000L)
						shipOverdue = shipOverdue.add(fare);
				}
				// Live late fee on unpaid base not yet frozen into a row.
				for (LogisticsPayableLine l : lines) {
					if (l.getKind() == LogisticsPayableLineKind.BASE_TRIP
							&& l.getStatus() != LogisticsPayableLineStatus.PAID) {
						BigDecimal live = liveLateFee(l, now);
						shipOpen = shipOpen.add(live);
						if (l.getDueDate() != null && l.getDueDate().before(now))
							shipOverdue = shipOverdue.add(live);
					}
				}
			} else {
				List<FreightInvoice> invs = freightRepo.findByShipmentShipmentId(s.getShipmentId())
						.stream().filter(f -> f.getStatus() != FreightInvoiceStatus.CANCELLED).toList();
				if (!invs.isEmpty()) {
					for (FreightInvoice f : invs) {
						if (f.getStatus() == FreightInvoiceStatus.PAID)
							continue;
						BigDecimal amt = orZero(f.getTotalAmount());
						shipOpen = shipOpen.add(amt);
						if (f.getDueDate() != null && f.getDueDate().before(now))
							shipOverdue = shipOverdue.add(amt);
					}
				} else if (fare.signum() > 0) {
					shipOpen = shipOpen.add(fare);
					long deliveredAt = deliveredAtOf(s);
					if (System.currentTimeMillis() - deliveredAt > 7L * 86_400_000L)
						shipOverdue = shipOverdue.add(fare);
				}
			}
			open = open.add(shipOpen);
			overdue = overdue.add(shipOverdue);
		}
		Map<String, BigDecimal> m = new LinkedHashMap<>();
		m.put("open", open);
		m.put("overdue", overdue);
		m.put("penalties", penalties);
		return m;
	}

	/** First delivered trip's resolved settlement currency; USD fallback. */
	private String settlementCurrency(List<Shipment> delivered) {
		for (Shipment s : delivered) {
			String c = billingService.tripCurrency(s);
			if (c != null)
				return c;
		}
		return "USD";
	}

	private BigDecimal liveLateFee(LogisticsPayableLine line, Timestamp now) {
		if (line.getDueDate() == null)
			return BigDecimal.ZERO;
		long daysLate = java.time.Duration.between(line.getDueDate().toInstant().plus(
				java.time.Duration.ofDays(1)), now.toInstant()).toDays() + 1;
		if (daysLate <= 0)
			return BigDecimal.ZERO;
		BigDecimal accrued = line.getAmount().multiply(new BigDecimal("0.01"))
				.multiply(BigDecimal.valueOf(daysLate));
		BigDecimal frozen = lineRepo.findByShipmentId(line.getShipmentId()).stream()
				.filter(l -> l.getKind() == LogisticsPayableLineKind.LATE_FEE)
				.map(l -> orZero(l.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
		return accrued.subtract(frozen).max(BigDecimal.ZERO);
	}

	private long deliveredAtOf(Shipment s) {
		if (s.getActualArrival() != null)
			return s.getActualArrival().getTime();
		if (s.getUpdatedAt() != null)
			return s.getUpdatedAt().getTime();
		return System.currentTimeMillis();
	}

	private List<Partnership> logisticsPartnershipsOf(Long orgId) {
		List<Partnership> out = new ArrayList<>();
		for (Partnership p : partnershipRepo.findByPrimaryOrgAccountId(orgId, Pageable.unpaged()).getContent()) {
			if ("LOGISTICS".equals(p.getPartnershipType()))
				out.add(p);
		}
		for (Partnership p : partnershipRepo.findBySecondaryOrgAccountId(orgId, Pageable.unpaged()).getContent()) {
			if ("LOGISTICS".equals(p.getPartnershipType()))
				out.add(p);
		}
		return out;
	}

	private BigDecimal sumPo(List<PurchaseOrder> pos) {
		return pos.stream().map(p -> p.getTotalAmount() != null ? BigDecimal.valueOf(p.getTotalAmount())
				: BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private BigDecimal sumInv(List<Invoice> invoices, InvoiceStatus status) {
		return invoices.stream().filter(i -> i.getStatus() == status)
				.map(i -> orZero(i.getTotalAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private BigDecimal sumOverdue(List<Invoice> invoices, InvoiceStatus status) {
		Timestamp now = new Timestamp(System.currentTimeMillis());
		return invoices.stream().filter(i -> i.getStatus() == status && i.getDueDate() != null
				&& i.getDueDate().before(now)).map(i -> orZero(i.getTotalAmount()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private List<Map<String, Object>> aging(List<Invoice> invoices, InvoiceStatus status) {
		long now = System.currentTimeMillis();
		long[] buckets = new long[4];
		BigDecimal[] sums = { BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO };
		for (Invoice i : invoices) {
			if (i.getStatus() != status || i.getDueDate() == null)
				continue;
			long days = (now - i.getDueDate().getTime()) / 86_400_000L;
			if (days < 0)
				continue;
			int b = days <= 30 ? 0 : days <= 60 ? 1 : days <= 90 ? 2 : 3;
			buckets[b]++;
			sums[b] = sums[b].add(orZero(i.getTotalAmount()));
		}
		String[] labels = { "0-30", "31-60", "61-90", "90+" };
		List<Map<String, Object>> out = new ArrayList<>();
		for (int b = 0; b < 4; b++) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("bucket", labels[b]);
			m.put("count", buckets[b]);
			m.put("amount", sums[b]);
			out.add(m);
		}
		return out;
	}

	private List<Map<String, Object>> monthlyTrend(List<Invoice> invoices, Timestamp since) {
		Map<String, BigDecimal> byMonth = new TreeMap<>();
		for (Invoice i : invoices) {
			java.util.Date d = i.getInvoiceDate() != null ? i.getInvoiceDate() : toDate(i.getCreatedAt());
			if (d == null)
				continue;
			if (since != null && d.getTime() < since.getTime())
				continue;
			String key = String.format("%tY-%tm", d, d);
			byMonth.merge(key, orZero(i.getTotalAmount()), BigDecimal::add);
		}
		List<Map<String, Object>> out = new ArrayList<>();
		byMonth.forEach((k, v) -> {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("period", k);
			m.put("amount", v);
			out.add(m);
		});
		return out;
	}

	private List<Map<String, Object>> shipmentTrend(List<Shipment> ships, Timestamp since) {
		Map<String, BigDecimal> byMonth = new TreeMap<>();
		for (Shipment s : ships) {
			if (s.getStatus() != ShipmentStatus.DELIVERED
					&& s.getStatus() != ShipmentStatus.PARTIALLY_DELIVERED)
				continue;
			java.util.Date d = toDate(s.getUpdatedAt());
			if (d == null || (since != null && d.getTime() < since.getTime()))
				continue;
			String key = String.format("%tY-%tm", d, d);
			BigDecimal fare = billingService.tripFare(s);
			byMonth.merge(key, fare, BigDecimal::add);
		}
		List<Map<String, Object>> out = new ArrayList<>();
		byMonth.forEach((k, v) -> {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("period", k);
			m.put("amount", v);
			out.add(m);
		});
		return out;
	}

	private List<Map<String, Object>> topSuppliers(List<Invoice> invoices) {
		Map<String, BigDecimal> by = new LinkedHashMap<>();
		for (Invoice i : invoices) {
			String name = "Unknown";
			if (i.getPurchaseOrder() != null && i.getPurchaseOrder().getSupplier() != null
					&& i.getPurchaseOrder().getSupplier().getBusinessName() != null) {
				name = i.getPurchaseOrder().getSupplier().getBusinessName();
			}
			by.merge(name, orZero(i.getTotalAmount()), BigDecimal::add);
		}
		return top(by, 5);
	}

	private List<Map<String, Object>> topBuyers(List<PurchaseOrder> orders) {
		Map<String, BigDecimal> by = new LinkedHashMap<>();
		for (PurchaseOrder o : orders) {
			String name = o.getBuyerOrg() != null ? "org-" + o.getBuyerOrg().getAccountId() : "Unknown";
			by.merge(name,
					o.getTotalAmount() != null ? BigDecimal.valueOf(o.getTotalAmount()) : BigDecimal.ZERO,
					BigDecimal::add);
		}
		return top(by, 5);
	}

	private List<Map<String, Object>> top(Map<String, BigDecimal> by, int n) {
		return by.entrySet().stream()
				.sorted(Map.Entry.<String, BigDecimal>comparingByValue(Comparator.reverseOrder())).limit(n)
				.map(e -> {
					Map<String, Object> m = new LinkedHashMap<>();
					m.put("name", e.getKey());
					m.put("amount", e.getValue());
					return m;
				}).toList();
	}

	private Map<String, Object> funnel(List<PurchaseOrder> pos, List<Invoice> invoices) {
		Map<String, Object> f = new LinkedHashMap<>();
		f.put("ordered", sumPo(pos));
		BigDecimal received = pos.stream()
				.filter(p -> p.getStatus() == PurchaseOrderStatus.RECEIVED
						|| p.getStatus() == PurchaseOrderStatus.INVOICED
						|| p.getStatus() == PurchaseOrderStatus.PAID)
				.map(p -> p.getTotalAmount() != null ? BigDecimal.valueOf(p.getTotalAmount()) : BigDecimal.ZERO)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		f.put("received", received);
		f.put("invoiced", invoices.stream().map(i -> orZero(i.getTotalAmount())).reduce(BigDecimal.ZERO,
				BigDecimal::add));
		f.put("paid", sumInv(invoices, InvoiceStatus.PAID));
		return f;
	}

	private Timestamp since(String period) {
		if (period == null || "all".equalsIgnoreCase(period))
			return null;
		int days = switch (period.toLowerCase()) {
			case "7d" -> 7;
			case "90d" -> 90;
			case "12m" -> 365;
			default -> 30;
		};
		return Timestamp.valueOf(LocalDate.now().minusDays(days).atStartOfDay());
	}

	private ResponseEntity<?> page(List<Map<String, Object>> rows, Pageable pageable) {
		rows.sort((a, b) -> String.valueOf(b.get("date")).compareTo(String.valueOf(a.get("date"))));
		int size = pageable.getPageSize();
		int fromIdx = Math.min(pageable.getPageNumber() * size, rows.size());
		int toIdx = Math.min(fromIdx + size, rows.size());
		Map<String, Object> res = new LinkedHashMap<>();
		res.put("content", rows.subList(fromIdx, toIdx));
		res.put("totalElements", rows.size());
		res.put("totalPages", size == 0 ? 1 : (int) Math.ceil((double) rows.size() / size));
		res.put("number", pageable.getPageNumber());
		res.put("size", size);
		return ResponseEntity.ok(res);
	}

	private static boolean matchStatus(String filter, String actual) {
		if (filter == null || filter.isBlank() || "ALL".equalsIgnoreCase(filter))
			return true;
		if ("OVERDUE".equalsIgnoreCase(filter))
			return true; // handled by caller bucket when needed
		return filter.equalsIgnoreCase(actual);
	}

	private static boolean matchSearch(String q, String... fields) {
		if (q == null || q.isBlank())
			return true;
		String needle = q.trim().toLowerCase();
		for (String f : fields) {
			if (f != null && f.toLowerCase().contains(needle))
				return true;
		}
		return false;
	}

	private static boolean inRange(java.util.Date d, Date from, Date to) {
		if (d == null)
			return from == null && to == null;
		if (from != null && d.before(from))
			return false;
		if (to != null && d.after(to))
			return false;
		return true;
	}

	private static java.util.Date toDate(Timestamp ts) {
		return ts != null ? new java.util.Date(ts.getTime()) : null;
	}

	private static BigDecimal orZero(BigDecimal v) {
		return v != null ? v : BigDecimal.ZERO;
	}
}
