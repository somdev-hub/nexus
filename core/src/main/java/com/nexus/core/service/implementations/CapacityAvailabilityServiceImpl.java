package com.nexus.core.service.implementations;

import java.sql.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.nexus.core.exception.ValidationException;
import com.nexus.core.model.entities.CapacityReservation;
import com.nexus.core.model.entities.ProductionCapacityCalendar;
import com.nexus.core.model.entities.SupplierCatalog;
import com.nexus.core.repository.CapacityReservationRepo;
import com.nexus.core.repository.ProductVariantRepo;
import com.nexus.core.repository.ProductionCapacityRepo;
import com.nexus.core.repository.SupplierCatalogRepo;
import com.nexus.core.service.interfaces.CapacityAvailabilityService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CapacityAvailabilityServiceImpl implements CapacityAvailabilityService {

	private static final double EPSILON = 0.000001;

	/**
	 * Sentinel window bounds so the matching query never receives a null
	 * parameter — PostgreSQL cannot infer the data type of a null-bound
	 * parameter in a bare `? IS NULL` context ("could not determine data type
	 * of parameter $N"). A missing window therefore means "unbounded", which
	 * the sentinels express without changing the result.
	 */
	private static final Date WINDOW_OPEN = Date.valueOf(java.time.LocalDate.of(1900, 1, 1));
	private static final Date WINDOW_CLOSE = Date.valueOf(java.time.LocalDate.of(9999, 12, 31));

	private final ProductionCapacityRepo capacityRepo;
	private final CapacityReservationRepo reservationRepo;
	private final SupplierCatalogRepo catalogRepo;
	private final ProductVariantRepo variantRepo;

	@Override
	public void validateAvailability(Long supplierOrgId, Long catalogId, Long variantId, Double quantity,
			Date windowStart, Date windowEnd, String label) {
		double available = remainingAcross(supplierOrgId, catalogId, variantId, windowStart, windowEnd);
		if (quantity != null && available + EPSILON < quantity) {
			throw new ValidationException("Insufficient remaining capacity for " + describe(label)
					+ (hasWindow(windowStart, windowEnd) ? " in the requested period " + windowStart + " → " + windowEnd : "")
					+ ": requested " + quantity + ", available " + Math.round(available * 1e6) / 1e6
					+ ". Add or free up capacity in the capacity calendar.");
		}
	}

	@Override
	public void reserveCapacity(Long supplierOrgId, String sourceType, Long sourceId, Long catalogId,
			Long variantId, Double quantity, Date windowStart, Date windowEnd, String label) {
		if (quantity == null || quantity <= 0) {
			return;
		}
		List<ProductionCapacityCalendar> rows = matchedRows(supplierOrgId, catalogId, variantId, windowStart, windowEnd);
		double available = rows.stream().mapToDouble(this::remainingOf).sum();
		if (available + EPSILON < quantity) {
			throw new ValidationException("Insufficient remaining capacity for " + describe(label)
					+ (hasWindow(windowStart, windowEnd) ? " in the requested period " + windowStart + " → " + windowEnd : "")
					+ ": requested " + quantity + ", available " + Math.round(available * 1e6) / 1e6
					+ ". Add or free up capacity in the capacity calendar.");
		}
		// Allocate chronologically, one ledger row per capacity row touched.
		double leftToBook = quantity;
		for (ProductionCapacityCalendar row : rows) {
			if (leftToBook <= EPSILON) {
				break;
			}
			double take = Math.min(leftToBook, remainingOf(row));
			if (take <= EPSILON) {
				continue;
			}
			row.setAllocatedCapacity((row.getAllocatedCapacity() != null ? row.getAllocatedCapacity() : 0.0) + take);
			capacityRepo.save(row);

			CapacityReservation reservation = new CapacityReservation();
			reservation.setSourceType(sourceType);
			reservation.setSourceId(sourceId);
			reservation.setCapacityId(row.getCapacityId());
			reservation.setSupplierOrgId(supplierOrgId);
			reservation.setCatalogId(catalogId);
			reservation.setVariantId(variantId);
			reservation.setQuantity(take);
			reservationRepo.save(reservation);

			leftToBook -= take;
		}
	}

	@Override
	public void releaseReservations(String sourceType, Long sourceId) {
		List<CapacityReservation> reservations = reservationRepo.findBySourceTypeAndSourceIdAndIsActiveTrue(sourceType, sourceId);
		for (CapacityReservation reservation : reservations) {
			reservation.setIsActive(false);
			reservationRepo.save(reservation);
			capacityRepo.findById(reservation.getCapacityId()).ifPresent(row -> {
				double allocated = row.getAllocatedCapacity() != null ? row.getAllocatedCapacity() : 0.0;
				row.setAllocatedCapacity(Math.max(0.0, allocated - reservation.getQuantity()));
				capacityRepo.save(row);
			});
		}
		if (!reservations.isEmpty()) {
			log.debug("Released {} capacity reservation(s) for {} {}", reservations.size(), sourceType, sourceId);
		}
	}

	private List<ProductionCapacityCalendar> matchedRows(Long supplierOrgId, Long catalogId, Long variantId,
			Date windowStart, Date windowEnd) {
		Date start = windowStart != null ? windowStart : WINDOW_OPEN;
		Date end = windowEnd != null ? windowEnd : WINDOW_CLOSE;
		if (variantId == null) {
			return capacityRepo.findMatchedCapacityAllVariants(supplierOrgId, catalogId, start, end);
		}
		return capacityRepo.findMatchedCapacity(supplierOrgId, catalogId, variantId, start, end);
	}

	@Override
	public double remainingCapacity(Long supplierOrgId, Long catalogId, Long variantId, Date windowStart,
			Date windowEnd) {
		return remainingAcross(supplierOrgId, catalogId, variantId, windowStart, windowEnd);
	}

	private double remainingAcross(Long supplierOrgId, Long catalogId, Long variantId, Date windowStart, Date windowEnd) {
		return matchedRows(supplierOrgId, catalogId, variantId, windowStart, windowEnd).stream()
				.mapToDouble(this::remainingOf)
				.sum();
	}

	private double remainingOf(ProductionCapacityCalendar row) {
		double available = row.getAvailableCapacity() != null ? row.getAvailableCapacity() : 0.0;
		double allocated = row.getAllocatedCapacity() != null ? row.getAllocatedCapacity() : 0.0;
		return Math.max(0.0, available - allocated);
	}

	private boolean hasWindow(Date windowStart, Date windowEnd) {
		return windowStart != null || windowEnd != null;
	}

	private String describe(String label) {
		return label != null ? "'" + label + "'" : "the item";
	}
}
