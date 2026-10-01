package com.nexus.core.payload;

import java.sql.Timestamp;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PartnershipInvitationDto {
	// Primary key — must be present so clients can accept/reject/withdraw
	// a specific invitation. (Its absence used to make the UI send NaN ids.)
	private Long invitationId;

	// Set server-side from OrganizationContextHolder in PartnershipInvitationService;
	// must not be required from the client or @Valid rejects every create.
	private Long invitingOrg;

	@NotNull(message = "Invited organization is required")
	private Long invitedOrg;

	private com.nexus.core.model.enums.PartnershipInvitationStatus status;

	private String partnershipContext;

	private String proposedTerms;

	private Double proposedDiscountRate;

	private Timestamp invitedAt;

	private Timestamp respondedAt;

	private Timestamp expiresAt;

	private String invitedBy;

	private String respondedBy;

	private String rejectionReason;

	private Long retailerSupplierId;

	private Timestamp createdAt;

	private Timestamp updatedAt;

	private Integer version;
}