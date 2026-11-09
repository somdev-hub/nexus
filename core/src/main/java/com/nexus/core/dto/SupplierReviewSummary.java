package com.nexus.core.dto;

import lombok.Data;

@Data
public class SupplierReviewSummary {
    private Long supplierOrgId;
    private String supplierName;
    private Double avgRating;
    private Long reviewCount;
    private Double avgQuality;
}