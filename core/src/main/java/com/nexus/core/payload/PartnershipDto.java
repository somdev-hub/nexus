package com.nexus.core.payload;

import java.sql.Timestamp;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PartnershipDto {
	// Primary key — clients need it to reference a specific partnership.
	private Long partnershipId;

	@NotNull(message = "Primary organization is required")
	private Long primaryOrg;

	@NotNull(message = "Secondary organization is required")
	private Long secondaryOrg;

	// Display names (populated server-side; clients must not send them).
	private String primaryOrgName;

	private String secondaryOrgName;

	private String partnershipTerm;

	private String partnershipType;

	// SHORT_TERM | LONG_TERM for supplier-logistics partnerships.
	private String partnershipTermType;

	private Timestamp validityStart;

	private Timestamp validityEnd;

	private Long linkedCapacityForecastId;

	private Double discountRate;

	private com.nexus.core.model.enums.PartnershipStatus status;

	private Timestamp startDate;

	private Timestamp endDate;

	private Timestamp revivedDate;

	// Partnership Agreement - DMS document reference
	private Long agreementDocumentId;

	// Partnership Invitation reference
	private Long invitationId;

	// Audit trail
	private Timestamp createdAt;

	private Timestamp updatedAt;

	private String createdBy;

	private String updatedBy;

	// Version for optimistic locking
	private Integer version;
}