package com.nexus.core.service.implementations;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import com.nexus.core.model.entities.LogisticsBillingCycle;
import com.nexus.core.model.entities.LogisticsPayQuote;
import com.nexus.core.model.entities.LogisticsPayableLine;
import com.nexus.core.model.entities.Partnership;
import com.nexus.core.model.entities.Shipment;
import com.nexus.core.model.enums.LogisticsPayableLineKind;
import com.nexus.core.model.enums.LogisticsPayableLineStatus;
import com.nexus.core.model.enums.ShipmentStatus;
import com.nexus.core.repository.LogisticsBillingCycleRepo;
import com.nexus.core.repository.LogisticsPayQuoteRepo;
import com.nexus.core.repository.LogisticsPayableLineRepo;
import com.nexus.core.repository.PartnershipRepo;
import com.nexus.core.repository.ShipmentRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.interfaces.LogisticsBillingService;

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
			row.put("lane", s != null ? s.getPickupLocation() + " → " + s.getDeliveryLocation() : null);
			row.put("deliveredAt", line.getDueDate());
			row.put("baseAmount", line.getAmount());
			row.put("paidAmount", line.getPaidAmount());
			row.put("outstandingBase", line.getAmount().subtract(orZero(line.getPaidAmount())));
			row.put("lateFeeAccrued", liveFee(line, now));
			row.put("status", line.getStatus().name());
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
			for (LogisticsPayableLine l : lines) {
				if (l.getKind() == LogisticsPayableLineKind.BASE_TRIP
						&& l.getStatus() != LogisticsPayableLineStatus.PAID) {
					fees = fees.add(liveFee(l, now));
				}
			}
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
			if (lineRepo.findByPartnershipIdAndShipmentIdAndKind(p.getPartnershipId(), s.getShipmentId(),
					LogisticsPayableLineKind.BASE_TRIP).isPresent())
				continue;
			BigDecimal fare = s.getActualFreightCost() != null ? BigDecimal.valueOf(s.getActualFreightCost())
					: (s.getFreightCost() != null ? BigDecimal.valueOf(s.getFreightCost()) : BigDecimal.ZERO);
			LogisticsPayableLine l = new LogisticsPayableLine();
			l.setPartnershipId(p.getPartnershipId());
			l.setShipmentId(s.getShipmentId());
			l.setCycleId("LONG_TERM".equals(p.getPartnershipTermType()) ? windowOf(p, deliveredAt(s)) : null);
			l.setKind(LogisticsPayableLineKind.BASE_TRIP);
			l.setAmount(fare);
			l.setPaidAmount(BigDecimal.ZERO);
			l.setStatus(LogisticsPayableLineStatus.OPEN);
			l.setDueDate(new Timestamp(deliveredAt(s)));
			l.setCurrency(s.getCurrency() != null ? s.getCurrency() : "USD");
			try {
				lineRepo.save(l);
			} catch (org.springframework.dao.DataIntegrityViolationException dup) {
				log.debug("trip line race for shipment {}", s.getShipmentId());
			}
		}
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
			m.put("lines", objectMapper.readValue(q.getPayloadJson(), List.class));
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
