package com.nexus.core.model.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_production_orders", schema = "core")
public class ProductionOrder extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "production_order_id")
	private Long productionOrderId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", referencedColumnName = "product_id", nullable = false)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Product product;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "warehouse_id", referencedColumnName = "warehouse_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Warehouse warehouse;

	private Long org;

	private Double quantity;

	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private ProductionStatus status = ProductionStatus.PLANNED;

	private Double revenueAmount;

	@Column(name = "deadline")
	private Timestamp deadline;

	@Column(name = "started_at")
	private Timestamp startedAt;

	@Column(name = "completed_at")
	private Timestamp completedAt;

	@Column(name = "notes", columnDefinition = "TEXT")
	private String notes;

	@OneToMany(mappedBy = "productionOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<ProductionOrderMaterial> materials = new ArrayList<>();

	public enum ProductionStatus {
		PLANNED, IN_PROGRESS, COMPLETED, CANCELLED
	}
}