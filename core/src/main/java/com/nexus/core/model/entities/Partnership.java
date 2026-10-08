package com.nexus.core.model.entities;

import com.nexus.core.model.enums.PartnershipStatus;
import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_partnerships", schema = "core")
public class Partnership extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "partnership_id")
	private Long partnershipId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "primary_org_id", referencedColumnName = "account_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Account primaryOrg;

	private String primaryOrgName;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "secondary_org_id", referencedColumnName = "account_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Account secondaryOrg;

	private String secondaryOrgName;

	private String partnershipType;

	private String partnershipTerm;

	// Supplier-logistics term model: SHORT_TERM (bound to a routing-capacity
	// period) or LONG_TERM (logistics may extend its routing-capacity period
	// so the partner supplier keeps flexibility).
	private String partnershipTermType;

	private Timestamp validityStart;

	private Timestamp validityEnd;

	// CapacityForecast.forecastId this partnership proposal was anchored to.
	private Long linkedCapacityForecastId;

	// LONG_TERM wish list carried over from the accepted invitation:
	// JSON array of {from, to, capacity} plus totals.
	private String desiredRoutesJson;

	private Double desiredCapacity;

	private String desiredCapacityUnit;

	private Double discountRate;

	@Enumerated(EnumType.STRING)
	private PartnershipStatus status;

	private Timestamp startDate;

	private Timestamp endDate;

	private Timestamp revivedDate;

	// Partnership Agreement - DMS document reference
	private Long agreementDocumentId;

	// Partnership Invitation reference
	private Long invitationId;
}
