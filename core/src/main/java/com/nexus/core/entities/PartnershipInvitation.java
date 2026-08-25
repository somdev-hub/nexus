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
@Table(name = "t_partnership_invitations", schema = "core")
public class PartnershipInvitation extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "invitation_id")
	private Long invitationId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "inviting_org_id", referencedColumnName = "account_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Account invitingOrg;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "invited_org_id", referencedColumnName = "account_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Account invitedOrg;

	@Enumerated(EnumType.STRING)
	private PartnershipInvitationStatus status;

	private String partnershipContext; // RETAILER_SUPPLIER, RETAILER_LOGISTICS, SUPPLIER_LOGISTICS

	private String proposedTerms;

	private Double proposedDiscountRate;

	private Timestamp invitedAt;

	private Timestamp respondedAt;

	private Timestamp expiresAt;

	private String invitedBy;

	private String respondedBy;

	private String rejectionReason;
}