package com.nexus.core.payload;

import java.sql.Timestamp;

import lombok.Data;

@Data
public class PartnershipDto {
	private Long primaryOrg;
	private Long secondaryOrg;
	private String partnershipTerm;
	private Double discountRate;
	private com.nexus.core.entities.PartnershipStatus status;
	private Timestamp startDate;
	private Timestamp endDate;
	private Timestamp revivedDate;
}