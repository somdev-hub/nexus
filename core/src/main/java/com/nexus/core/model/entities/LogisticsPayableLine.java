package com.nexus.core.model.entities;

import java.math.BigDecimal;
import java.sql.Timestamp;

import com.nexus.core.model.enums.LogisticsPayableLineKind;
import com.nexus.core.model.enums.LogisticsPayableLineStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * One billable money line for supplier → logistics settlement.
 * BASE_TRIP rows are created lazily when a shipment hits DELIVERED.
 * LATE_FEE rows are frozen at payment time (1%/day, simple). CYCLE_PENALTY
 * rows are raised when a LONG_TERM statement closes unpaid (2% of the
 * missed statement base, simple). Partial payments accumulate on
 * paidAmount with oldest-first allocation; status flips PAID at full cover.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_logistics_payable_lines", schema = "core", uniqueConstraints = {
		@UniqueConstraint(name = "uk_payline_trip_kind", columnNames = { "partnershipId", "shipmentId",
				"kind" })
})
public class LogisticsPayableLine extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long lineId;

	private Long partnershipId;

	private Long shipmentId;

	/** Billing window id, e.g. 2026-09 / 2026-09-W3. Null for trip lines. */
	private String cycleId;

	@Enumerated(EnumType.STRING)
	private LogisticsPayableLineKind kind;

	@Column(precision = 19, scale = 4)
	private BigDecimal amount;

	@Column(precision = 19, scale = 4)
	private BigDecimal paidAmount = BigDecimal.ZERO;

	@Enumerated(EnumType.STRING)
	private LogisticsPayableLineStatus status = LogisticsPayableLineStatus.OPEN;

	private Timestamp dueDate;

	private String paidByIntentRef;

	private String currency = "USD";
}
