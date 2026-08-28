package com.nexus.core.payload;

import com.nexus.core.entities.StockMovement;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * DTO for StockMovement entity.
 * Provides audit trail for inventory movements.
 */
@Data
public class StockMovementDto {

	@NotNull(message = "Stock ID is required")
	private Long stockId;

	@NotNull(message = "Movement type is required")
	private StockMovement.MovementType movementType;

	@NotNull(message = "Quantity is required")
	private Double quantity;

	private Double quantityBefore;

	private Double quantityAfter;

	private Double unitCost;

	private Double totalCost;

	private String referenceType;

	private Long referenceId;

	private String referenceNumber;

	private String batchNumber;

	private Date expiryDate;

	private Long fromWarehouseId;

	private Long toWarehouseId;

	private String reason;

	private String notes;

	private String createdBy;

	// Computed fields for response
	private String materialCode;
	private String materialName;
	private String warehouseCode;
	private String fromWarehouseCode;
	private String toWarehouseCode;
	private Timestamp createdAt;
}