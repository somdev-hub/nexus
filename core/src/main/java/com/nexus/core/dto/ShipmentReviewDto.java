package com.nexus.core.dto;

import java.sql.Timestamp;

import lombok.Data;

@Data
public class ShipmentReviewDto {
    private Long reviewId;
    private Long shipmentId;
    private String shipmentNumber;
    private String supplierName;
    private Integer rating;
    private Double qualityScore;
    private Timestamp expectedDelivery;
    private Timestamp actualDelivery;
    private Double expectedQuantity;
    private Double actualQuantity;
    private String comments;
    private Timestamp createdAt;
}