package com.nexus.core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.nexus.core.model.entities.ShipmentStop;
public interface ShipmentStopRepo extends JpaRepository<ShipmentStop, Long> {

	Page<ShipmentStop> findByShipmentShipmentId(Long shipmentId, Pageable pageable);

	Page<ShipmentStop> findByShipmentShipmentIdAndStopType(Long shipmentId, com.nexus.core.model.enums.StopType stopType,
			Pageable pageable);

	Page<ShipmentStop> findByShipmentShipmentIdAndStopStatus(Long shipmentId,
			com.nexus.core.model.enums.StopStatus stopStatus, Pageable pageable);
}