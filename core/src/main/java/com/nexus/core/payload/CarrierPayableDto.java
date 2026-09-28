package com.nexus.core.payload;

import com.nexus.core.model.enums.PayableStatus;
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
public class CarrierPayableDto {
    private Long payableId;
    private String payableNumber;
    private Long shipmentId;
    private String carrierName;
    private PayableStatus status;
    private BigDecimal payableAmount;
    private BigDecimal paidAmount;
    private String currency;
    private Date dueDate;
    private String pmsReferenceId;
    private String notes;
}
