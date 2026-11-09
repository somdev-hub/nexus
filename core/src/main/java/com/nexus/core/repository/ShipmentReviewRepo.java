package com.nexus.core.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nexus.core.model.entities.ShipmentReview;

public interface ShipmentReviewRepo extends JpaRepository<ShipmentReview, Long> {
    boolean existsByShipmentId(Long shipmentId);
    List<ShipmentReview> findByRetailerOrgId(Long retailerOrgId);
    Page<ShipmentReview> findByRetailerOrgId(Long retailerOrgId, Pageable pageable);
}