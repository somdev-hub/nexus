package com.nexus.core.payload;

import java.sql.Timestamp;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PartnershipDto {
	@NotNull(message = "Primary organization is required")
	private Long primaryOrg;

	@NotNull(message = "Secondary organization is required")
	private Long secondaryOrg;

	private String partnershipTerm;

	private Double discountRate;

	private com.nexus.core.entities.PartnershipStatus status;

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