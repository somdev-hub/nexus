package com.nexus.core.service.interfaces;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.http.ResponseEntity;

import com.nexus.core.model.entities.Shipment;

public interface LogisticsBillingService {

	/**
	 * Resolved billable fare for a shipment, in quintuple fallback order:
	 * shipped actual/estimated cost, else the logistics route-capacity price
	 * matched by lane (partnership-linked first), adjusted by the partnership
	 * contract discount. Zero when neither source yields a price.
	 */
	BigDecimal tripFare(Shipment shipment);

	/** Currency the resolved fare is denominated in (shipment's own or the matched lane's). */
	String tripCurrency(Shipment shipment);

	/**
	 * Flat driver fees of the matched route capacity — the driver's per-trip
	 * commission. Zero when no lane matches or the lane charges no driver fees.
	 */
	TripDriverFees tripDriverFees(Shipment shipment);

	/** Flat driver fee amount plus the currency the matched capacity denominates it in. */
	record TripDriverFees(BigDecimal amount, String currency) {
	}

	/**
	 * Human lane label for a shipment: the matched route-capacity's
	 * originLane → destinationLane (city names) when the shipment rides a
	 * known lane; the shipment's locations shortened to their city part
	 * otherwise.
	 */
	String laneLabel(Shipment shipment);

	ResponseEntity<?> getOpenTrips(Long partnershipId);

	ResponseEntity<?> getStatements(Long partnershipId);

	ResponseEntity<?> createBatchQuote(Map<String, Object> body);

	ResponseEntity<?> getQuote(String quoteRef);

	ResponseEntity<?> markPaid(Map<String, Object> body);
}
