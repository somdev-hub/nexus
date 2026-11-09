package com.nexus.core.model.entities;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "shipment_review", schema = "core")
public class ShipmentReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    @Column(name = "shipment_id", nullable = false, unique = true)
    private Long shipmentId;

    @Column(name = "retailer_org_id", nullable = false)
    private Long retailerOrgId;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "quality_score")
    private Double qualityScore;

    @Column(name = "expected_delivery")
    private Timestamp expectedDelivery;

    @Column(name = "actual_delivery")
    private Timestamp actualDelivery;

    @Column(name = "expected_quantity")
    private Double expectedQuantity;

    @Column(name = "actual_quantity")
    private Double actualQuantity;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "created_at")
    private Timestamp createdAt;
}