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
@Table(name = "t_supplier_qualifications", schema = "core")
public class SupplierQualification extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "qualification_id")
	private Long qualificationId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier_id", referencedColumnName = "supplier_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Supplier supplier;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "retailer_org_id", referencedColumnName = "account_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Account retailerOrg;

	@Enumerated(EnumType.STRING)
	private QualificationStatus status; // PENDING, IN_PROGRESS, APPROVED, REJECTED, EXPIRED

	private String complianceDocuments; // JSON array of document references (DMS document IDs)

	private String complianceNotes;

	private Double score; // 0.0 to 100.0

	private String assessedBy;

	private Timestamp assessedAt;

	private Timestamp validUntil;

	private String rejectionReason;
}