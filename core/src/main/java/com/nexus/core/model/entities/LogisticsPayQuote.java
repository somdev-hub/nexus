package com.nexus.core.model.entities;

import java.math.BigDecimal;
import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Frozen payable quote. The checkout may only collect against a live
 * (OPEN, unexpired) quote and the amount is locked to grandTotal; partial
 * payment is expressed at mark-paid time and allocated oldest-first.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_logistics_pay_quotes", schema = "core")
public class LogisticsPayQuote extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long quoteDbId;

	@Column(nullable = false, unique = true)
	private String quoteRef;

	@Column(nullable = false, unique = true)
	private String idempotencyKey;

	private Long partnershipId;

	/** TRIP_BATCH or STATEMENT. */
	private String kind;

	/** JSON snapshot of quoted lines (line ids + frozen amounts). */
	@Column(columnDefinition = "TEXT")
	private String payloadJson;

	@Column(precision = 19, scale = 4)
	private BigDecimal baseTotal = BigDecimal.ZERO;

	@Column(precision = 19, scale = 4)
	private BigDecimal feeTotal = BigDecimal.ZERO;

	@Column(precision = 19, scale = 4)
	private BigDecimal penaltyTotal = BigDecimal.ZERO;

	@Column(precision = 19, scale = 4)
	private BigDecimal grandTotal = BigDecimal.ZERO;

	/** OPEN, COMPLETED, EXPIRED. */
	private String status = "OPEN";

	private Timestamp expiresAt;

	private String intentRef;
}
