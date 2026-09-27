package com.nexus.core.payload;

import com.nexus.core.entities.QuoteStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentQuoteDto {
    private Long quoteId;
    private String quoteNumber;
    private Long shipmentId;
    private QuoteStatus status;
    @NotNull(message = "Base rate is required")
    private BigDecimal baseRate;
    private BigDecimal fuelSurcharge;
    private BigDecimal accessorialCharges;
    private String accessorialDetails;
    private BigDecimal totalAmount;
    private String currency;
    private Date validUntil;
    private String notes;
}
