package com.nexus.core.entities;

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
@Table(name = "t_suppliers", schema = "core")
public class Supplier extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "supplier_id")
	private Long supplierId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id", referencedColumnName = "account_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Account account;

	private String businessName;

	private String category; // e.g., "Electronics", "Textiles", "Food & Beverage"

	private String location; // City, Country

	private String website;

	private String contactPerson;

	private String contactEmail;

	private String contactPhone;

	private String certifications; // Comma-separated certifications (ISO 9001, ISO 14001, etc.)

	private Double rating; // 0.0 to 5.0

	private Integer totalOrders;

	private Double onTimeDeliveryRate; // Percentage

	private Double qualityScore; // Percentage

	@Enumerated(EnumType.STRING)
	private SupplierStatus status; // ACTIVE, INACTIVE, PENDING_VERIFICATION, SUSPENDED
}