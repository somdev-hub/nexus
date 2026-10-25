package com.nexus.core.service.interfaces;

import java.sql.Date;

/**
 * Capacity-calendar matching rules shared by quotations and orders.
 *
 * Matching semantics: a demand for catalog item C (optionally variant V)
 * over window [start, end] is satisfied by the supplier's capacity rows
 * whose catalog is C and whose variant is either unrestricted (null) or
 * exactly V, whose period overlaps the window, and whose
 * available − allocated has slack. Quotations validate against the
 * quotation's validity window; orders against the delivery date.
 */
public interface CapacityAvailabilityService {

	/** Throws ValidationException with a user-facing message on shortfall. */
	void validateAvailability(Long supplierOrgId, Long catalogId, Long variantId, Double quantity,
			Date windowStart, Date windowEnd, String label);

	/** Sum of remaining (available − allocated) capacity of matching rows. */
	double remainingCapacity(Long supplierOrgId, Long catalogId, Long variantId, Date windowStart, Date windowEnd);

	/** validateAvailability + commits the allocation and ledger rows atomically. */
	void reserveCapacity(Long supplierOrgId, String sourceType, Long sourceId, Long catalogId,
			Long variantId, Double quantity, Date windowStart, Date windowEnd, String label);

	/** Deactivates the source's ledger rows and restores allocated capacity. */
	void releaseReservations(String sourceType, Long sourceId);
}
