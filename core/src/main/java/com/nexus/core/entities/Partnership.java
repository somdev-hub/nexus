package com.nexus.core.entities;

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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "secondary_org_id", referencedColumnName = "account_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Account secondaryOrg;

	private String partnershipTerm;

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
