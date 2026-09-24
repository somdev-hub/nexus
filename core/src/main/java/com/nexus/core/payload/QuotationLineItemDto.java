package com.nexus.core.payload;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationLineItemDto {

    private Long lineId;

    private Long catalogId;

    private String catalogName;

    private String description;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", message = "Quantity must be non-negative")
    private Double quantity;

    @NotNull(message = "Unit price is required")
    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    private String notes;
}
