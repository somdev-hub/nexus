package com.nexus.core.model.entities;

import java.math.BigDecimal;
import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * One LONG_TERM billing window for a partnership. Windows anchor on the
 * partnership startDate with the partnership paymentFrequency (WEEKLY /
 * MONTHLY); generated lazily on read. A window whose base lines are not
 * fully paid by cycleEnd raises one CYCLE_PENALTY (2% of base, simple)
 * onto the next window exactly once (penaltyApplied guard).
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_logistics_billing_cycles", schema = "core", uniqueConstraints = {
		@UniqueConstraint(name = "uk_billcycle_pship_start", columnNames = { "partnershipId", "cycleStart" })
})
public class LogisticsBillingCycle extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long cycleDbId;

	private Long partnershipId;

	/** Human window id, e.g. 2026-09 or 2026-09-W3. */
	private String cycleId;

	private String frequency;

	private Timestamp cycleStart;

	private Timestamp cycleEnd;

	/** OPEN, CLOSED, PAID. */
	private String status = "OPEN";

	@Column(precision = 19, scale = 4)
	private BigDecimal baseTotal = BigDecimal.ZERO;

	@Column(precision = 19, scale = 4)
	private BigDecimal penaltyAmount = BigDecimal.ZERO;

	@Column(precision = 19, scale = 4)
	private BigDecimal paidAmount = BigDecimal.ZERO;

	private Boolean penaltyApplied = false;
}
