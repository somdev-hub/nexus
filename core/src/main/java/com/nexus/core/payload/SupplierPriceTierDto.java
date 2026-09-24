package com.nexus.core.payload;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierPriceTierDto {

    private Long tierId;

    @NotNull(message = "Catalog ID is required")
    private Long catalogId;

    private String catalogName;

    private String tierName;

    private Double minQuantity;

    private Double maxQuantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", message = "Unit price must be non-negative")
    private BigDecimal unitPrice;

    private String customerSegment;

    private Long contractId;

    private Date validFrom;

    private Date validTo;

    private String currency;

    private Timestamp createdAt;
    private Timestamp updatedAt;
}
