package com.nexus.core.service.implementations;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.core.annotation.LogActivity;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.exception.ValidationException;
import com.nexus.core.model.entities.CapacityForecast;
import com.nexus.core.model.entities.LogisticsBillingCycle;
import com.nexus.core.model.entities.LogisticsPayQuote;
import com.nexus.core.model.entities.LogisticsPayableLine;
import com.nexus.core.model.entities.Partnership;
import com.nexus.core.model.entities.Shipment;
import com.nexus.core.model.enums.CapacityUnit;
import com.nexus.core.model.enums.LogisticsPayableLineKind;
import com.nexus.core.model.enums.LogisticsPayableLineStatus;
import com.nexus.core.model.enums.ShipmentStatus;
import com.nexus.core.repository.CapacityForecastRepo;
import com.nexus.core.repository.LogisticsBillingCycleRepo;
import com.nexus.core.repository.LogisticsPayQuoteRepo;
import com.nexus.core.repository.LogisticsPayableLineRepo;
import com.nexus.core.repository.PartnershipRepo;
import com.nexus.core.repository.ShipmentRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.interfaces.LogisticsBillingService;
import com.nexus.core.utils.RouteCapacityMatcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Supplier → logistics settlement engine. SHORT_TERM partnerships settle
 * trip-wise (batchable); LONG_TERM partnerships settle per cycle (WEEKLY /
 * MONTHLY from partnership.paymentFrequency). Late fee 1%/day simple on the
 * trip base; missed statements add 2% of the missed base onto the next
 * window (simple, once). Partials allocate oldest-first. The checkout only
 * ever pays a frozen, unexpired quote.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogisticsBillingServiceImpl implements LogisticsBillingService {

	private static final BigDecimal LATE_FEE_PER_DAY = new BigDecimal("0.01");
	private static final BigDecimal MISS_PENALTY_RATE = new BigDecimal("0.02");
	private static final long QUOTE_TTL_MIN = 15;

	private final PartnershipRepo partnershipRepo;
	private final ShipmentRepo shipmentRepo;
	private final LogisticsPayableLineRepo lineRepo;
	private final LogisticsBillingCycleRepo cycleRepo;
	private final LogisticsPayQuoteRepo quoteRepo;
	private final CapacityForecastRepo capacityRepo;
	private final ObjectMapper objectMapper;

	// ── open trips ──────────────────────────────────────────────
	@Override
	@Transactional
	@LogActivity("Get Open Logistics Trips")
	public ResponseEntity<?> getOpenTrips(Long partnershipId) {
		Partnership p = requirePartnership(partnershipId);
		ensureTripLines(p);
		Timestamp now = now();
		List<Map<String, Object>> trips = new ArrayList<>();
		for (LogisticsPayableLine line : openBaseLines(p.getPartnershipId())) {
			Shipment s = line.getShipmentId() != null
					? shipmentRepo.findById(line.getShipmentId()).orElse(null)
					: null;
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("lineId", line.getLineId());
			row.put("shipmentId", line.getShipmentId());
			row.put("shipmentNumber", s != null ? s.getShipmentNumber() : null);
			row.put("lane", s != null ? laneLabel(s) : null);
			row.put("deliveredAt", line.getDueDate());
			row.put("baseAmount", line.getAmount());
			row.put("paidAmount", line.getPaidAmount());
			row.put("outstandingBase", line.getAmount().subtract(orZero(line.getPaidAmount())));
			row.put("lateFeeAccrued", liveFee(line, now));
			row.put("status", line.getStatus().name());
			row.put("currency", line.getCurrency());
			row.put("pricing", s != null ? fareBreakdown(s, p) : Map.of());
			trips.add(row);
		}
		Map<String, Object> res = new LinkedHashMap<>();
		res.put("partnershipId", p.getPartnershipId());
		res.put("termType", p.getPartnershipTermType());
		res.put("trips", trips);
		return ResponseEntity.ok(res);
	}

	// ── statements ────────────────────────────────────────────
	@Override
	@Transactional
	@LogActivity("Get Logistics Statements")
	public ResponseEntity<?> getStatements(Long partnershipId) {
		Partnership p = requirePartnership(partnershipId);
		ensureTripLines(p);
		ensureCycles(p);
		Timestamp now = now();
		List<Map<String, Object>> out = new ArrayList<>();
		for (LogisticsBillingCycle c : cycleRepo.findByPartnershipIdOrderByCycleStartAsc(p.getPartnershipId())) {
			List<LogisticsPayableLine> lines = lineRepo
					.findByPartnershipIdAndCycleIdOrderByDueDateAsc(p.getPartnershipId(), c.getCycleId());
			BigDecimal base = sumByKind(lines, LogisticsPayableLineKind.BASE_TRIP);
			BigDecimal basePaid = sumPaidByKind(lines, LogisticsPayableLineKind.BASE_TRIP);
			BigDecimal penalty = sumByKind(lines, LogisticsPayableLineKind.CYCLE_PENALTY);
			BigDecimal penaltyPaid = sumPaidByKind(lines, LogisticsPayableLineKind.CYCLE_PENALTY);
			BigDecimal fees = BigDecimal.ZERO;
			String rowCurrency = null;
			for (LogisticsPayableLine l : lines) {
				if (rowCurrency == null && l.getCurrency() != null)
					rowCurrency = l.getCurrency();
				if (l.getKind() == LogisticsPayableLineKind.BASE_TRIP
						&& l.getStatus() != LogisticsPayableLineStatus.PAID) {
					fees = fees.add(liveFee(l, now));
				}
			}
			if (rowCurrency == null)
				rowCurrency = "USD";
			c.setBaseTotal(base);
			cycleRepo.save(c);
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("cycleId", c.getCycleId());
			row.put("frequency", c.getFrequency());
			row.put("cycleStart", c.getCycleStart());
			row.put("cycleEnd", c.getCycleEnd());
			row.put("status", deriveCycleStatus(c, base, basePaid));
			row.put("baseTotal", base);
			row.put("basePaid", basePaid);
			row.put("penaltyTotal", penalty);
			row.put("penaltyPaid", penaltyPaid);
			row.put("lateFeesAccrued", fees);
			row.put("lineCount", lines.size());
			row.put("currency", rowCurrency);
			out.add(row);
		}
		Map<String, Object> res = new LinkedHashMap<>();
		res.put("partnershipId", p.getPartnershipId());
		res.put("frequency", freqOf(p));
		res.put("statements", out);
		return ResponseEntity.ok(res);
	}

	// ── frozen quote ──────────────────────────────────────────
	@Override
	@Transactional
	@LogActivity("Create Logistics Pay Quote")
	@SuppressWarnings("unchecked")
	public ResponseEntity<?> createBatchQuote(Map<String, Object> body) {
		Long partnershipId = num(body.get("partnershipId"));
		if (partnershipId == null)
			throw new ValidationException("partnershipId is required");
		Partnership p = requirePartnership(partnershipId);
		ensureTripLines(p);
		if ("LONG_TERM".equals(p.getPartnershipTermType()))
			ensureCycles(p);

		String key = str(body.get("idempotencyKey"));
		if (key == null || key.isBlank())
			key = "logi-quote-" + UUID.randomUUID();
		var replay = quoteRepo.findByIdempotencyKey(key);
		if (replay.isPresent() && "OPEN".equals(replay.get().getStatus())
				&& replay.get().getExpiresAt().after(now())) {
			return ResponseEntity.ok(quoteDto(replay.get()));
		}

		String kind = str(body.get("kind"));
		Timestamp now = now();
		List<LogisticsPayableLine> baseLines = new ArrayList<>();
		if ("STATEMENT".equalsIgnoreCase(kind)) {
			String cycleId = str(body.get("cycleId"));
			if (cycleId == null)
				throw new ValidationException("cycleId is required for STATEMENT quotes");
			for (LogisticsPayableLine l : lineRepo.findByPartnershipIdAndCycleIdOrderByDueDateAsc(
					p.getPartnershipId(), cycleId)) {
				if (l.getKind() != LogisticsPayableLineKind.BASE_TRIP
						&& l.getKind() != LogisticsPayableLineKind.CYCLE_PENALTY)
					continue;
				if (l.getStatus() == LogisticsPayableLineStatus.PAID)
					continue;
				baseLines.add(l);
			}
		} else {
			List<Object> ids = (List<Object>) body.get("shipmentIds");
			if (ids == null || ids.isEmpty())
				throw new ValidationException("shipmentIds is required for TRIP_BATCH quotes");
			for (Object id : ids) {
				Long sid = num(id);
				var line = lineRepo.findByPartnershipIdAndShipmentIdAndKind(
						p.getPartnershipId(), sid, LogisticsPayableLineKind.BASE_TRIP).orElse(null);
				if (line == null)
					throw new ValidationException("Shipment " + sid + " has no billable trip line");
				if (line.getStatus() == LogisticsPayableLineStatus.PAID)
					continue;
				baseLines.add(line);
			}
		}
		baseLines.removeIf(l -> l.getAmount().subtract(orZero(l.getPaidAmount()))
				.compareTo(BigDecimal.ZERO) <= 0);
		if (baseLines.isEmpty())
			throw new ValidationException("Nothing to bill — all selected lines are PAID");
		baseLines.sort(Comparator.comparing(LogisticsPayableLine::getDueDate,
				Comparator.nullsLast(Comparator.naturalOrder())));

		// Freeze: base remainder + live late-fee remainder per trip (fee is
		// virtual here; the frozen LATE_FEE row is written at mark-paid).
		BigDecimal base = BigDecimal.ZERO, fee = BigDecimal.ZERO, pen = BigDecimal.ZERO;
		List<Map<String, Object>> snap = new ArrayList<>();
		for (LogisticsPayableLine l : baseLines) {
			BigDecimal rem = l.getAmount().subtract(orZero(l.getPaidAmount()));
			if (l.getKind() == LogisticsPayableLineKind.CYCLE_PENALTY)
				pen = pen.add(rem);
			else
				base = base.add(rem);
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("lineId", l.getLineId());
			m.put("shipmentId", l.getShipmentId());
			m.put("kind", l.getKind().name());
			m.put("due", l.getDueDate());
			m.put("quoted", rem);
			snap.add(m);
			if (l.getKind() == LogisticsPayableLineKind.BASE_TRIP) {
				BigDecimal feeRem = liveFee(l, now);
				if (feeRem.compareTo(BigDecimal.ZERO) > 0) {
					fee = fee.add(feeRem);
					Map<String, Object> f = new LinkedHashMap<>();
					f.put("lineId", null);
					f.put("shipmentId", l.getShipmentId());
					f.put("kind", LogisticsPayableLineKind.LATE_FEE.name());
					f.put("due", l.getDueDate());
					f.put("quoted", feeRem);
					snap.add(f);
				}
			}
		}
		LogisticsPayQuote q = new LogisticsPayQuote();
		q.setQuoteRef("LQ-" + System.currentTimeMillis() + "-"
				+ UUID.randomUUID().toString().substring(0, 6).toUpperCase());
		q.setIdempotencyKey(key);
		q.setPartnershipId(p.getPartnershipId());
		q.setKind(pen.signum() > 0 || "STATEMENT".equalsIgnoreCase(kind) ? "STATEMENT" : "TRIP_BATCH");
		try {
			q.setPayloadJson(objectMapper.writeValueAsString(snap));
		} catch (Exception e) {
			throw new ValidationException("Could not freeze quote");
		}
		q.setBaseTotal(base);
		q.setFeeTotal(fee);
		q.setPenaltyTotal(pen);
		q.setGrandTotal(base.add(fee).add(pen));
		q.setStatus("OPEN");
		q.setExpiresAt(new Timestamp(System.currentTimeMillis() + QUOTE_TTL_MIN * 60 * 1000L));
		try {
			q = quoteRepo.save(q);
		} catch (org.springframework.dao.DataIntegrityViolationException dup) {
			var winner = quoteRepo.findByIdempotencyKey(key).orElseThrow(() -> dup);
			return ResponseEntity.ok(quoteDto(winner));
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(quoteDto(q));
	}

	@Override
	@Transactional
	public ResponseEntity<?> getQuote(String quoteRef) {
		LogisticsPayQuote q = quoteRepo.findByQuoteRef(quoteRef)
				.orElseThrow(() -> new ResourceNotFoundException("LogisticsPayQuote", "quoteRef", quoteRef));
		if ("OPEN".equals(q.getStatus()) && q.getExpiresAt().before(now())) {
			q.setStatus("EXPIRED");
			quoteRepo.save(q);
		}
		return ResponseEntity.ok(quoteDto(q));
	}

	// ── mark paid: partials allocate oldest-first ─────────────
	@Override
	@Transactional
	@LogActivity("Mark Logistics Quote Paid")
	public ResponseEntity<?> markPaid(Map<String, Object> body) {
		String quoteRef = str(body.get("quoteRef"));
		BigDecimal amountPaid = body.get("amountPaid") != null
				? new BigDecimal(String.valueOf(body.get("amountPaid")))
				: null;
		String intentRef = str(body.get("intentRef"));
		if (quoteRef == null || amountPaid == null || amountPaid.compareTo(BigDecimal.ZERO) <= 0)
			throw new ValidationException("quoteRef and a positive amountPaid are required");
		LogisticsPayQuote q = quoteRepo.findByQuoteRef(quoteRef)
				.orElseThrow(() -> new ResourceNotFoundException("LogisticsPayQuote", "quoteRef", quoteRef));
		if (!"OPEN".equals(q.getStatus()))
			throw new ValidationException("Quote is " + q.getStatus());
		if (q.getExpiresAt().before(now())) {
			q.setStatus("EXPIRED");
			quoteRepo.save(q);
			throw new ValidationException("Quote expired — re-quote to pay");
		}
		if (amountPaid.compareTo(q.getGrandTotal()) > 0)
			throw new ValidationException("amountPaid exceeds quoted grandTotal");

		List<Map<String, Object>> snap;
		try {
			snap = objectMapper.readValue(q.getPayloadJson(), new TypeReference<>() {
			});
		} catch (Exception e) {
			throw new ValidationException("Corrupt quote payload");
		}
		BigDecimal left = amountPaid;
		Map<Long, BigDecimal> feeCovered = new LinkedHashMap<>();
		List<Map<String, Object>> allocation = new ArrayList<>();
		for (Map<String, Object> e : snap) {
			if (left.compareTo(BigDecimal.ZERO) <= 0)
				break;
			BigDecimal quoted = new BigDecimal(String.valueOf(e.get("quoted")));
			BigDecimal cover = left.min(quoted);
			if ("LATE_FEE".equals(e.get("kind"))) {
				Long shipId = e.get("shipmentId") != null ? num(e.get("shipmentId")) : null;
				if (shipId != null && cover.signum() > 0) {
					feeCovered.merge(shipId, cover, BigDecimal::add);
					left = left.subtract(cover);
					Map<String, Object> a = new LinkedHashMap<>();
					a.put("kind", "LATE_FEE");
					a.put("shipmentId", shipId);
					a.put("applied", cover);
					allocation.add(a);
				}
				continue;
			}
			Long lineId = num(e.get("lineId"));
			LogisticsPayableLine line = lineRepo.findById(lineId)
					.orElseThrow(() -> new ResourceNotFoundException("LogisticsPayableLine", "lineId", lineId));
			BigDecimal outstanding = line.getAmount().subtract(orZero(line.getPaidAmount()));
			BigDecimal apply = cover.min(outstanding.max(BigDecimal.ZERO));
			if (apply.signum() > 0) {
				line.setPaidAmount(orZero(line.getPaidAmount()).add(apply));
				line.setPaidByIntentRef(intentRef);
				line.setStatus(line.getPaidAmount().compareTo(line.getAmount()) >= 0
						? LogisticsPayableLineStatus.PAID
						: LogisticsPayableLineStatus.PARTIAL);
				lineRepo.save(line);
				left = left.subtract(apply);
				Map<String, Object> a = new LinkedHashMap<>();
				a.put("lineId", lineId);
				a.put("kind", line.getKind().name());
				a.put("applied", apply);
				a.put("lineStatus", line.getStatus().name());
				allocation.add(a);
			}
		}
		for (var fc : feeCovered.entrySet()) {
			upsertFeeLine(q.getPartnershipId(), fc.getKey(), fc.getValue(), intentRef);
		}
		q.setIntentRef(intentRef);
		boolean fullyCovered = q.getGrandTotal().subtract(amountPaid).abs()
				.compareTo(new BigDecimal("0.01")) < 0;
		if (fullyCovered)
			q.setStatus("COMPLETED");
		quoteRepo.save(q);
		refreshCycleStatuses(q.getPartnershipId());
		Map<String, Object> res = new LinkedHashMap<>();
		res.put("quoteRef", quoteRef);
		res.put("amountPaid", amountPaid);
		res.put("allocation", allocation);
		res.put("fullyCovered", fullyCovered);
		res.put("quoteStatus", q.getStatus());
		return ResponseEntity.ok(res);
	}

	// ── internals ─────────────────────────────────────────────
	private BigDecimal liveFee(LogisticsPayableLine line, Timestamp now) {
		if (line.getKind() != LogisticsPayableLineKind.BASE_TRIP)
			return BigDecimal.ZERO;
		if (line.getStatus() == LogisticsPayableLineStatus.PAID)
			return BigDecimal.ZERO;
		if (line.getDueDate() == null)
			return BigDecimal.ZERO;
		long daysLate = Duration.between(line.getDueDate().toInstant().plus(Duration.ofDays(1)),
				now.toInstant()).toDays() + 1;
		if (daysLate <= 0)
			return BigDecimal.ZERO;
		BigDecimal accrued = line.getAmount().multiply(LATE_FEE_PER_DAY)
				.multiply(BigDecimal.valueOf(daysLate)).setScale(4, RoundingMode.HALF_UP);
		BigDecimal frozen = lineRepo.findByShipmentId(line.getShipmentId()).stream()
				.filter(l -> l.getKind() == LogisticsPayableLineKind.LATE_FEE)
				.map(l -> orZero(l.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
		return accrued.subtract(frozen).max(BigDecimal.ZERO);
	}

	private void ensureTripLines(Partnership p) {
		List<Shipment> delivered = shipmentRepo
				.findByPartnershipPartnershipId(p.getPartnershipId(), Pageable.unpaged()).getContent()
				.stream().filter(s -> s.getStatus() == ShipmentStatus.DELIVERED).toList();
		for (Shipment s : delivered) {
			var existing = lineRepo.findByPartnershipIdAndShipmentIdAndKind(p.getPartnershipId(),
					s.getShipmentId(), LogisticsPayableLineKind.BASE_TRIP).orElse(null);
			Fare fare = fareFor(s, p);
			if (existing != null) {
				// Self-heal unpaid lines: re-price from the currently
				// resolved fare so the row, its dropdown breakdown and any
				// quote always agree — covers legacy zero rows (billed from
				// shipment.freightCost only), stale amounts from lane
				// pricing/cargo data updated after line creation, and
				// currency stamped from the shipment before capacity
				// currency existed. Never touches lines with payments applied.
				boolean amountDiffers = orZero(existing.getAmount())
						.compareTo(fare.amount()) != 0;
				boolean currencyDiffers = fare.currency() != null
						&& !fare.currency().equals(existing.getCurrency());
				if (orZero(existing.getPaidAmount()).signum() == 0
						&& existing.getStatus() != LogisticsPayableLineStatus.PAID
						&& fare.amount().signum() > 0
						&& (amountDiffers || currencyDiffers)) {
					existing.setAmount(fare.amount());
					existing.setCurrency(fare.currency());
					lineRepo.save(existing);
				}
				continue;
			}
			LogisticsPayableLine l = new LogisticsPayableLine();
			l.setPartnershipId(p.getPartnershipId());
			l.setShipmentId(s.getShipmentId());
			l.setCycleId("LONG_TERM".equals(p.getPartnershipTermType()) ? windowOf(p, deliveredAt(s)) : null);
			l.setKind(LogisticsPayableLineKind.BASE_TRIP);
			l.setAmount(fare.amount());
			l.setPaidAmount(BigDecimal.ZERO);
			l.setStatus(LogisticsPayableLineStatus.OPEN);
			l.setDueDate(new Timestamp(deliveredAt(s)));
			l.setCurrency(fare.currency());
			try {
				lineRepo.save(l);
			} catch (org.springframework.dao.DataIntegrityViolationException dup) {
				log.debug("trip line race for shipment {}", s.getShipmentId());
			}
		}
	}

	// ── trip pricing ──────────────────────────────────────────
	/**
	 * Billable fare priority: the shipment's recorded (actual, else
	 * estimated) freight cost when it carries a real price; otherwise the
	 * price of the logistics route capacity matched to the trip lane —
	 * unitPrice scaled by shipped cargo, plus the lane's flat surcharges,
	 * reduced by the partnership contract discountRate. Zero when neither
	 * source yields a price.
	 */
	@Override
	public BigDecimal tripFare(Shipment s) {
		if (s == null)
			return BigDecimal.ZERO;
		return fareFor(s, s.getPartnership()).amount();
	}

	/** Currency the fare is denominated in: shipment's own, else the matched capacity's. */
	@Override
	public String tripCurrency(Shipment s) {
		if (s == null)
			return null;
		return fareFor(s, s.getPartnership()).currency();
	}

	/** Flat driver fees of the matched route capacity + the currency they are in. */
	@Override
	public TripDriverFees tripDriverFees(Shipment s) {
		if (s == null)
			return new TripDriverFees(BigDecimal.ZERO, billCurrency(s));
		CapacityForecast lane = matchLane(s, s.getPartnership());
		if (lane == null)
			return new TripDriverFees(BigDecimal.ZERO, billCurrency(s));
		BigDecimal fees = lane.getDriverFees() != null
				? BigDecimal.valueOf(lane.getDriverFees()).setScale(2, RoundingMode.HALF_UP)
				: BigDecimal.ZERO;
		return new TripDriverFees(fees,
				lane.getCurrency() != null ? lane.getCurrency() : billCurrency(s));
	}

	private BigDecimal tripFareFor(Shipment s, Partnership p) {
		return fareFor(s, p).amount();
	}

	@Override
	public String laneLabel(Shipment s) {
		if (s == null)
			return null;
		CapacityForecast lane = matchLane(s, s.getPartnership());
		String from = lane != null ? lane.getOriginLane() : null;
		String to = lane != null ? lane.getDestinationLane() : null;
		if (from != null && !from.isBlank() && to != null && !to.isBlank())
			return from + " → " + to;
		return shortCity(s.getPickupLocation() != null ? s.getPickupLocation() : s.getPickupAddress())
				+ " → "
				+ shortCity(s.getDeliveryLocation() != null ? s.getDeliveryLocation() : s.getDeliveryAddress());
	}

	/** City part of an address (first comma segment). */
	private String shortCity(String addr) {
		if (addr == null)
			return null;
		String t = addr.trim();
		int comma = t.indexOf(',');
		return comma > 0 ? t.substring(0, comma).trim() : t;
	}

	/** Fare plus the currency it is denominated in. */
	private record Fare(BigDecimal amount, String currency) {
	}

	private Fare fareFor(Shipment s, Partnership p) {
		BigDecimal own = ownFare(s);
		if (own.signum() > 0)
			return new Fare(own, billCurrency(s));
		return capacityFare(s, p);
	}

	private BigDecimal ownFare(Shipment s) {
		if (s.getActualFreightCost() != null && s.getActualFreightCost() > 0)
			return BigDecimal.valueOf(s.getActualFreightCost());
		if (s.getFreightCost() != null && s.getFreightCost() > 0)
			return BigDecimal.valueOf(s.getFreightCost());
		return BigDecimal.ZERO;
	}

	private Fare capacityFare(Shipment s, Partnership p) {
		CapacityForecast lane = matchLane(s, p);
		if (lane == null || lane.getUnitPrice() == null)
			return new Fare(BigDecimal.ZERO, billCurrency(s));
		BigDecimal price = BigDecimal.valueOf(lane.getUnitPrice())
				.multiply(cargoQuantity(lane, s));
		for (Double flat : List.of(lane.getFuelSurcharge(), lane.getDriverFees(), lane.getMiscPrice()))
			if (flat != null)
				price = price.add(BigDecimal.valueOf(flat));
		price = price.multiply(discountFactor(p)).setScale(2, RoundingMode.HALF_UP);
		return new Fare(price, lane.getCurrency() != null ? lane.getCurrency() : billCurrency(s));
	}

	/**
	 * Lane resolution, widest last: capacity linked by the partnership
	 * contract, then partnership-private capacities, then the logistics
	 * org's own lanes, then the public marketplace. Mirrors the marketplace
	 * shipment view so both agree on what a lane costs.
	 */
	private CapacityForecast matchLane(Shipment s, Partnership p) {
		Partnership part = p != null ? p : s.getPartnership();
		String pickup = s.getPickupLocation() != null ? s.getPickupLocation() : s.getPickupAddress();
		String delivery = s.getDeliveryLocation() != null ? s.getDeliveryLocation() : s.getDeliveryAddress();
		if (part != null) {
			if (part.getLinkedCapacityForecastId() != null) {
				CapacityForecast linked = capacityRepo.findById(part.getLinkedCapacityForecastId()).orElse(null);
				if (linked != null && RouteCapacityMatcher.routeCovers(linked, pickup, delivery))
					return linked;
			}
			CapacityForecast hit = RouteCapacityMatcher.matchWithMode(
					capacityRepo.findByPartnershipPartnershipId(part.getPartnershipId(),
							Pageable.unpaged()).getContent(),
					pickup, delivery);
			if (hit != null)
				return hit;
		}
		if (s.getLogisticsOrg() != null && s.getLogisticsOrg().getAccountId() != null) {
			CapacityForecast hit = RouteCapacityMatcher.matchWithMode(
					capacityRepo.findByOrgWithFilters(s.getLogisticsOrg().getAccountId(), null, null,
							Pageable.unpaged()).getContent(),
					pickup, delivery);
			if (hit != null)
				return hit;
		}
		return RouteCapacityMatcher.matchWithMode(
				capacityRepo.findMarketplaceAvailabilities(null, Pageable.unpaged()).getContent(),
				pickup, delivery);
	}

	/**
	 * Cargo quantity billed in the lane's capacity unit: weight/volume ops
	 * consume the shipment totals (converted KG↔LBS↔TONNES, m³↔ft³↔L↔gal),
	 * unitized lanes (pallets/containers) are billed per package. With no
	 * cargo facts the lane's unitPrice is treated as a flat per-trip fare.
	 */
	private BigDecimal cargoQuantity(CapacityForecast c, Shipment s) {
		CapacityUnit unit = c.getCapacityUnit() != null ? c.getCapacityUnit() : CapacityUnit.KG;
		double qty = d(s.getTotalWeight());
		qty = switch (unit) {
			case KG -> d(s.getTotalWeight());
			case LBS -> d(s.getTotalWeight()) * 2.20462;
			case TONNES -> d(s.getTotalWeight()) / 1000.0;
			case CUBIC_METERS -> d(s.getTotalVolume());
			case CUBIC_FEET -> d(s.getTotalVolume()) * 35.3147;
			case LITRES -> d(s.getTotalVolume()) * 1000.0;
			case GALLONS -> d(s.getTotalVolume()) * 264.172;
			case PALLETS, SHIPPING_CONTAINER, FREIGHT_CONTAINER ->
				s.getTotalPackages() != null ? s.getTotalPackages() : 0.0;
		};
		if (qty <= 0)
			qty = 1;
		return BigDecimal.valueOf(qty);
	}

	/**
	 * Itemized price breakdown for one trip — mirrors exactly what
	 * {@link #fareFor} charges. CAPACITY trips show each lane component the
	 * logistics org priced (unit price × billed cargo, fuel surcharge, driver
	 * fees, misc) plus the partnership contract discount; own-priced shows the
	 * shipment's recorded cost; UNPRICED means no lane matched.
	 */
	private Map<String, Object> fareBreakdown(Shipment s, Partnership p) {
		Map<String, Object> b = new LinkedHashMap<>();
		BigDecimal own = ownFare(s);
		if (own.signum() > 0) {
			b.put("source", "SHIPMENT");
			b.put("currency", billCurrency(s));
			b.put("total", own);
			return b;
		}
		CapacityForecast lane = matchLane(s, p);
		if (lane == null || lane.getUnitPrice() == null) {
			b.put("source", "UNPRICED");
			b.put("currency", billCurrency(s));
			b.put("total", BigDecimal.ZERO);
			return b;
		}
		Fare fare = capacityFare(s, p);
		BigDecimal base = BigDecimal.valueOf(lane.getUnitPrice())
				.multiply(cargoQuantity(lane, s));
		b.put("source", "CAPACITY");
		b.put("currency", fare.currency());
		b.put("lane", lane.getOriginLane() + " → " + lane.getDestinationLane());
		if (lane.getTransportMode() != null)
			b.put("transportMode", lane.getTransportMode().name());
		if (lane.getEquipmentType() != null)
			b.put("equipmentType", lane.getEquipmentType().name());
		b.put("capacityUnit", lane.getCapacityUnit() != null
				? lane.getCapacityUnit().name() : "KG");
		b.put("unitPrice", lane.getUnitPrice());
		b.put("billedQuantity", cargoQuantity(lane, s));
		b.put("basePrice", base.setScale(2, RoundingMode.HALF_UP));
		if (lane.getFuelSurcharge() != null)
			b.put("fuelSurcharge", BigDecimal.valueOf(lane.getFuelSurcharge()));
		if (lane.getDriverFees() != null)
			b.put("driverFees", BigDecimal.valueOf(lane.getDriverFees()));
		if (lane.getMiscPrice() != null)
			b.put("miscPrice", BigDecimal.valueOf(lane.getMiscPrice()));
		BigDecimal subtotal = base;
		for (Double flat : List.of(lane.getFuelSurcharge(), lane.getDriverFees(), lane.getMiscPrice()))
			if (flat != null)
				subtotal = subtotal.add(BigDecimal.valueOf(flat));
		b.put("subtotal", subtotal.setScale(2, RoundingMode.HALF_UP));
		if (p != null && p.getDiscountRate() != null && p.getDiscountRate() > 0) {
			b.put("discountRate", Math.min(p.getDiscountRate(), 100.0));
			b.put("discount", subtotal.subtract(fare.amount())
					.setScale(2, RoundingMode.HALF_UP));
		}
		b.put("total", fare.amount());
		return b;
	}

	/** Partnership contract discount: discountRate is a percent (5 = 5% off). */
	private static BigDecimal discountFactor(Partnership p) {
		if (p == null || p.getDiscountRate() == null || p.getDiscountRate() <= 0)
			return BigDecimal.ONE;
		double rate = Math.min(p.getDiscountRate(), 100.0);
		return BigDecimal.ONE.subtract(BigDecimal.valueOf(rate).divide(
				BigDecimal.valueOf(100.0), 6, RoundingMode.HALF_UP));
	}

	private static double d(Double v) {
		return v != null ? v : 0.0;
	}

	/** Currency the line should carry: shipment's own, else USD default. */
	private static String billCurrency(Shipment s) {
		return s.getCurrency() != null ? s.getCurrency() : "USD";
	}

	private void ensureCycles(Partnership p) {
		String freq = freqOf(p);
		Timestamp anchorTs = p.getStartDate() != null ? p.getStartDate()
				: (p.getValidityStart() != null ? new Timestamp(p.getValidityStart().getTime())
						: new Timestamp(System.currentTimeMillis()));
		LocalDateTime cursor = anchorTs.toLocalDateTime();
		LocalDateTime nowL = LocalDateTime.now();
		int guard = 0;
		while (cursor.isBefore(nowL) && guard++ < 60) {
			LocalDateTime end = "WEEKLY".equals(freq) ? cursor.plusDays(7) : cursor.plusMonths(1);
			getOrCreateCycle(p, freq, cursor, end);
			cursor = end;
		}
		// Close past windows: unpaid base raises one 2% penalty onto the
		// next window (simple, once).
		List<LogisticsBillingCycle> cycles = cycleRepo.findByPartnershipIdOrderByCycleStartAsc(p.getPartnershipId());
		for (int i = 0; i < cycles.size(); i++) {
			LogisticsBillingCycle c = cycles.get(i);
			BigDecimal windowBase = sumByKind(lineRepo.findByPartnershipIdAndCycleIdOrderByDueDateAsc(
					p.getPartnershipId(), c.getCycleId()), LogisticsPayableLineKind.BASE_TRIP);
			BigDecimal windowPaid = sumPaidByKind(lineRepo.findByPartnershipIdAndCycleIdOrderByDueDateAsc(
					p.getPartnershipId(), c.getCycleId()), LogisticsPayableLineKind.BASE_TRIP);
			c.setBaseTotal(windowBase);
			c.setPaidAmount(windowPaid);
			cycleRepo.save(c);
			if (!Boolean.TRUE.equals(c.getPenaltyApplied())
					&& c.getCycleEnd().before(now())
					&& windowBase.subtract(windowPaid).compareTo(new BigDecimal("0.01")) > 0
					&& i + 1 < cycles.size()) {
				LogisticsBillingCycle next = cycles.get(i + 1);
				BigDecimal penalty = windowBase.multiply(MISS_PENALTY_RATE).setScale(4, RoundingMode.HALF_UP);
				upsertPenalty(p.getPartnershipId(), next.getCycleId(), penalty, next.getCycleEnd());
				next.setPenaltyAmount(orZero(next.getPenaltyAmount()).add(penalty));
				cycleRepo.save(next);
				c.setPenaltyApplied(true);
				cycleRepo.save(c);
			}
		}
	}

	private LogisticsBillingCycle getOrCreateCycle(Partnership p, String freq, LocalDateTime start,
			LocalDateTime end) {
		Timestamp ts = Timestamp.valueOf(start);
		return cycleRepo.findByPartnershipIdAndCycleStart(p.getPartnershipId(), ts).orElseGet(() -> {
			LogisticsBillingCycle c = new LogisticsBillingCycle();
			c.setPartnershipId(p.getPartnershipId());
			c.setCycleStart(ts);
			c.setCycleEnd(Timestamp.valueOf(end));
			c.setFrequency(freq);
			c.setCycleId("WEEKLY".equals(freq)
					? String.format("%04d-%02d-W%d", start.getYear(), start.getMonthValue(),
							(start.getDayOfMonth() - 1) / 7 + 1)
					: String.format("%04d-%02d", start.getYear(), start.getMonthValue()));
			c.setStatus(end.isBefore(LocalDateTime.now()) ? "CLOSED" : "OPEN");
			return cycleRepo.save(c);
		});
	}

	private String windowOf(Partnership p, long deliveredAtMs) {
		String freq = freqOf(p);
		Timestamp anchorTs = p.getStartDate() != null ? p.getStartDate()
				: (p.getValidityStart() != null ? new Timestamp(p.getValidityStart().getTime())
						: new Timestamp(deliveredAtMs));
		LocalDateTime a = anchorTs.toLocalDateTime();
		LocalDateTime d = new Timestamp(deliveredAtMs).toLocalDateTime();
		if ("WEEKLY".equals(freq)) {
			long weeks = Math.max(0, Duration.between(a, d).toDays() / 7);
			LocalDateTime w = a.plusWeeks(weeks);
			return String.format("%04d-%02d-W%d", w.getYear(), w.getMonthValue(),
					(w.getDayOfMonth() - 1) / 7 + 1);
		}
		return String.format("%04d-%02d", d.getYear(), d.getMonthValue());
	}

	private void upsertPenalty(Long partnershipId, String cycleId, BigDecimal amount, Timestamp due) {
		boolean exists = lineRepo.findByPartnershipIdAndCycleIdOrderByDueDateAsc(partnershipId, cycleId).stream()
				.anyMatch(l -> l.getKind() == LogisticsPayableLineKind.CYCLE_PENALTY);
		if (exists)
			return;
		LogisticsPayableLine l = new LogisticsPayableLine();
		l.setPartnershipId(partnershipId);
		l.setCycleId(cycleId);
		l.setKind(LogisticsPayableLineKind.CYCLE_PENALTY);
		l.setAmount(amount);
		l.setPaidAmount(BigDecimal.ZERO);
		l.setStatus(LogisticsPayableLineStatus.OPEN);
		l.setDueDate(due);
		lineRepo.save(l);
	}

	private void upsertFeeLine(Long partnershipId, Long shipmentId, BigDecimal coveredFee, String intentRef) {
		var existing = lineRepo.findByPartnershipIdAndShipmentIdAndKind(partnershipId, shipmentId,
				LogisticsPayableLineKind.LATE_FEE).orElse(null);
		if (existing == null) {
			var base = lineRepo.findByPartnershipIdAndShipmentIdAndKind(partnershipId, shipmentId,
					LogisticsPayableLineKind.BASE_TRIP).orElse(null);
			LogisticsPayableLine l = new LogisticsPayableLine();
			l.setPartnershipId(partnershipId);
			l.setShipmentId(shipmentId);
			l.setCycleId(base != null ? base.getCycleId() : null);
			l.setKind(LogisticsPayableLineKind.LATE_FEE);
			l.setAmount(coveredFee);
			l.setPaidAmount(coveredFee);
			l.setStatus(LogisticsPayableLineStatus.PAID);
			l.setDueDate(base != null ? base.getDueDate() : now());
			l.setPaidByIntentRef(intentRef);
			l.setCurrency(base != null && base.getCurrency() != null ? base.getCurrency() : "USD");
			lineRepo.save(l);
		} else {
			existing.setAmount(orZero(existing.getAmount()).add(coveredFee));
			existing.setPaidAmount(orZero(existing.getPaidAmount()).add(coveredFee));
			existing.setStatus(existing.getPaidAmount().compareTo(existing.getAmount()) >= 0
					? LogisticsPayableLineStatus.PAID
					: LogisticsPayableLineStatus.PARTIAL);
			lineRepo.save(existing);
		}
	}

	private void refreshCycleStatuses(Long partnershipId) {
		for (LogisticsBillingCycle c : cycleRepo.findByPartnershipIdOrderByCycleStartAsc(partnershipId)) {
			List<LogisticsPayableLine> lines = lineRepo.findByPartnershipIdAndCycleIdOrderByDueDateAsc(
					partnershipId, c.getCycleId());
			BigDecimal base = sumByKind(lines, LogisticsPayableLineKind.BASE_TRIP);
			BigDecimal paid = sumPaidByKind(lines, LogisticsPayableLineKind.BASE_TRIP);
			c.setBaseTotal(base);
			c.setPaidAmount(paid);
			if (base.subtract(paid).abs().compareTo(new BigDecimal("0.01")) < 0 && base.signum() > 0)
				c.setStatus("PAID");
			else if (c.getCycleEnd().before(now()))
				c.setStatus("CLOSED");
			else
				c.setStatus("OPEN");
			cycleRepo.save(c);
		}
	}

	private String deriveCycleStatus(LogisticsBillingCycle c, BigDecimal base, BigDecimal paid) {
		if (base.subtract(paid).abs().compareTo(new BigDecimal("0.01")) < 0 && base.signum() > 0)
			return "PAID";
		if (c.getCycleEnd() != null && c.getCycleEnd().before(now()))
			return "CLOSED";
		return "OPEN";
	}

	private Partnership requirePartnership(Long partnershipId) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Partnership p = partnershipRepo.findById(partnershipId)
				.orElseThrow(() -> new ResourceNotFoundException("Partnership", "partnershipId", partnershipId));
		boolean mine = (p.getPrimaryOrg() != null && orgId.equals(p.getPrimaryOrg().getAccountId()))
				|| (p.getSecondaryOrg() != null && orgId.equals(p.getSecondaryOrg().getAccountId()));
		if (!mine)
			throw new ValidationException("Partnership not visible to your organization");
		return p;
	}

	private List<LogisticsPayableLine> openBaseLines(Long partnershipId) {
		return lineRepo.findByPartnershipIdAndStatusInOrderByDueDateAsc(partnershipId,
				List.of(LogisticsPayableLineStatus.OPEN, LogisticsPayableLineStatus.PARTIAL)).stream()
				.filter(l -> l.getKind() == LogisticsPayableLineKind.BASE_TRIP).toList();
	}

	private Map<String, Object> quoteDto(LogisticsPayQuote q) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("quoteRef", q.getQuoteRef());
		m.put("idempotencyKey", q.getIdempotencyKey());
		m.put("partnershipId", q.getPartnershipId());
		m.put("kind", q.getKind());
		m.put("baseTotal", q.getBaseTotal());
		m.put("feeTotal", q.getFeeTotal());
		m.put("penaltyTotal", q.getPenaltyTotal());
		m.put("grandTotal", q.getGrandTotal());
		m.put("status", q.getStatus());
		m.put("expiresAt", q.getExpiresAt());
		try {
			List<?> payload = objectMapper.readValue(q.getPayloadJson(), List.class);
			m.put("lines", payload);
			// Enrich with the billed trips' facts (addresses, dates, logistics
			// company) so detail dialogs / invoices need no extra round-trip.
			Set<Long> ids = new LinkedHashSet<>();
			for (Object o : payload) {
				if (o instanceof Map<?, ?> e) {
					Long sid = num(e.get("shipmentId"));
					if (sid != null)
						ids.add(sid);
				}
			}
			Map<String, Object> shipments = new LinkedHashMap<>();
			for (Long sid : ids) {
				Shipment s = shipmentRepo.findById(sid).orElse(null);
				if (s == null)
					continue;
				Map<String, Object> f = new LinkedHashMap<>();
				f.put("shipmentId", sid);
				f.put("shipmentNumber", s.getShipmentNumber());
				f.put("lane", laneLabel(s));
				f.put("pickupLocation", s.getPickupLocation());
				f.put("pickupAddress", s.getPickupAddress());
				f.put("pickupDate", s.getPickupDate() != null
						? s.getPickupDate()
						: (s.getActualDeparture() != null ? s.getActualDeparture() : s.getEstimatedDeparture()));
				f.put("deliveryLocation", s.getDeliveryLocation());
				f.put("deliveryAddress", s.getDeliveryAddress());
				f.put("deliveryDate", s.getDeliveryDate() != null ? s.getDeliveryDate()
						: (s.getActualArrival() != null ? s.getActualArrival() : s.getEstimatedArrival()));
				f.put("deliveredAt", s.getActualArrival());
				f.put("status", s.getStatus() != null ? s.getStatus().name() : null);
				if (s.getLogisticsOrg() != null)
					f.put("logisticsOrg", s.getLogisticsOrg().getName());
				shipments.put(String.valueOf(sid), f);
			}
			m.put("shipments", shipments);
		} catch (Exception e) {
			m.put("lines", List.of());
		}
		return m;
	}

	private BigDecimal sumByKind(List<LogisticsPayableLine> lines, LogisticsPayableLineKind kind) {
		return lines.stream().filter(l -> l.getKind() == kind).map(l -> orZero(l.getAmount()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private BigDecimal sumPaidByKind(List<LogisticsPayableLine> lines, LogisticsPayableLineKind kind) {
		return lines.stream().filter(l -> l.getKind() == kind).map(l -> orZero(l.getPaidAmount()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static BigDecimal orZero(BigDecimal v) {
		return v != null ? v : BigDecimal.ZERO;
	}

	private static Timestamp now() {
		return new Timestamp(System.currentTimeMillis());
	}

	private static String freqOf(Partnership p) {
		return "WEEKLY".equals(p.getPaymentFrequency()) ? "WEEKLY" : "MONTHLY";
	}

	private static long deliveredAt(Shipment s) {
		if (s.getActualArrival() != null)
			return s.getActualArrival().getTime();
		if (s.getUpdatedAt() != null)
			return s.getUpdatedAt().getTime();
		return System.currentTimeMillis();
	}

	private static Long num(Object o) {
		if (o == null)
			return null;
		if (o instanceof Number n)
			return n.longValue();
		try {
			return Long.parseLong(String.valueOf(o));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String str(Object o) {
		return o != null ? String.valueOf(o) : null;
	}
}
