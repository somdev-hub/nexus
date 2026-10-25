package com.nexus.core.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Ledger of capacity committed against a supplier's capacity calendar. One
 * row per allocated slice: a quotation line consumed capacity when the
 * quotation was accepted (sourceType QUOTATION), or a purchase-order line
 * consumed capacity when the supplier acknowledged the order (sourceType
 * ORDER). Releasing a source deactivates its rows and restores
 * allocated_capacity on the matching ProductionCapacityCalendar rows.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_capacity_reservations", schema = "core")
public class CapacityReservation extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "reservation_id")
	private Long reservationId;

	/** QUOTATION or ORDER. */
	@Column(name = "source_type", nullable = false, length = 20)
	private String sourceType;

	/** SupplierQuotation.quotationId or PurchaseOrder.purchaseOrderId. */
	@Column(name = "source_id", nullable = false)
	private Long sourceId;

	/** ProductionCapacityCalendar row this slice was booked against. */
	@Column(name = "capacity_id", nullable = false)
	private Long capacityId;

	/** Owner org of the capacity rows (the supplier). */
	@Column(name = "supplier_org_id", nullable = false)
	private Long supplierOrgId;

	@Column(name = "catalog_id")
	private Long catalogId;

	@Column(name = "variant_id")
	private Long variantId;

	@Column(name = "quantity", nullable = false)
	private Double quantity;
}
