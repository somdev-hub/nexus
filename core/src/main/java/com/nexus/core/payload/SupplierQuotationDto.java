package com.nexus.core.payload;

import com.nexus.core.entities.SupplierQuotation;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierQuotationDto {

    private Long quotationId;

    private String quotationNumber;

    private Long buyerOrgId;

    private String buyerOrgName;

    private SupplierQuotation.QuotationStatus status;

    private Date validFrom;

    private Date validTo;

    private String terms;

    private String currency;

    private BigDecimal totalAmount;

    private Long parentQuotationId;

    private Integer versionNumber;

    private Long convertedToPoId;

    @NotEmpty(message = "At least one line item required")
    private List<QuotationLineItemDto> lineItems;

    private Long supplierOrgId;

    private Timestamp createdAt;
    private Timestamp updatedAt;
}
