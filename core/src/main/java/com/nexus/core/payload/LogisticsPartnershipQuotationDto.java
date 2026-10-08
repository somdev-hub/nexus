package com.nexus.core.payload;

import java.sql.Timestamp;

import com.nexus.core.model.enums.PartnershipQuotationStatus;

import lombok.Data;

@Data
public class LogisticsPartnershipQuotationDto {

    private Long quotationId;

    private String quotationNumber;

    private Long invitationId;

    private Long supplierOrgId;

    private String supplierOrgName;

    private Long logisticsOrgId;

    private String logisticsOrgName;

    private Long partnershipId;

    private PartnershipQuotationStatus status;

    private String routeLinesJson;

    private Double totalAmount;

    private String currency;

    private Timestamp validityStart;

    private Timestamp validityEnd;

    private String terms;
}
