package com.nexus.core.payload;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvanceShipmentNoticeDto {

    private Long asnId;

    private String asnNumber;

    private Long purchaseOrderId;

    private String poNumber;

    private Long buyerOrgId;

    private Long supplierOrgId;

    private Long shipmentId;

    private String status;

    private String notes;

    private Timestamp createdAt;

    private Timestamp updatedAt;
}
