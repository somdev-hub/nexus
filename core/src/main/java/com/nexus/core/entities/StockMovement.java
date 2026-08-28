package com.nexus.core.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * StockMovement entity for inventory audit trail.
 * Tracks all inventory movements (receipts, issues, adjustments, transfers).
 * Supports FR-RET-010, FR-RET-013 (batch/expiry tracking).
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_stock_movements", schema = "core")
public class StockMovement extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "movement_id")
	private Long movementId;

	@ManyToOne
	@JoinColumn(name = "stock_id", referencedColumnName = "stock_id", nullable = false)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Stock stock;

	@Enumerated(EnumType.STRING)
	@Column(name = "movement_type", nullable = false)
	private MovementType movementType;

	@Column(name = "quantity", nullable = false)
	private Double quantity;

	@Column(name = "quantity_before")
	private Double quantityBefore;

	@Column(name = "quantity_after")
	private Double quantityAfter;

	@Column(name = "unit_cost")
	private Double unitCost;

	@Column(name = "total_cost")
	private Double totalCost;

	@Column(name = "reference_type")
	private String referenceType; // PURCHASE_ORDER, GOODS_RECEIPT, INVOICE, TRANSFER, ADJUSTMENT, COUNT

	@Column(name = "reference_id")
	private Long referenceId;

	@Column(name = "reference_number")
	private String referenceNumber;

	@Column(name = "batch_number")
	private String batchNumber;

	@Column(name = "expiry_date")
	private java.sql.Date expiryDate;

	@Column(name = "from_warehouse_id")
	private Long fromWarehouseId;

	@Column(name = "to_warehouse_id")
	private Long toWarehouseId;

	@Column(name = "reason")
	private String reason;

	@Column(name = "notes", columnDefinition = "TEXT")
	private String notes;

	@Column(name = "created_by")
	private String createdBy;

	/**
	 * Movement types for inventory tracking
	 */
	public enum MovementType {
		// Inbound movements
		RECEIPT, // Goods receipt from supplier
		RETURN_FROM_CUSTOMER, // Customer return
		TRANSFER_IN, // Transfer from another warehouse
		ADJUSTMENT_IN, // Positive adjustment (cycle count, found stock)
		PRODUCTION_IN, // Production completion

		// Outbound movements
		ISSUE, // Issue to production/order
		SHIPMENT, // Shipment to customer
		TRANSFER_OUT, // Transfer to another warehouse
		ADJUSTMENT_OUT, // Negative adjustment (damage, loss, theft)
		RETURN_TO_SUPPLIER, // Return to supplier
		SCRAP, // Scrapped/disposed

		// Internal movements
		RESERVATION, // Stock reservation
		RELEASE_RESERVATION, // Release reservation
		CYCLE_COUNT, // Cycle count adjustment
		PHYSICAL_COUNT // Full physical inventory count
	}
}