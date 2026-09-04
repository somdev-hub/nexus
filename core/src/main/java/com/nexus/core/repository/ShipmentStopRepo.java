package com.nexus.core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.core.entities.ShipmentStop;

@Repository
public interface ShipmentStopRepo extends JpaRepository<ShipmentStop, Long> {

	Page<ShipmentStop> findByShipmentShipmentId(Long shipmentId, Pageable pageable);

	Page<ShipmentStop> findByShipmentShipmentIdAndStopType(Long shipmentId, com.nexus.core.entities.StopType stopType,
			Pageable pageable);

	Page<ShipmentStop> findByShipmentShipmentIdAndStopStatus(Long shipmentId,
			com.nexus.core.entities.StopStatus stopStatus, Pageable pageable);
}