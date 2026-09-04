package com.nexus.core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.core.entities.ShipmentDocument;

@Repository
public interface ShipmentDocumentRepo extends JpaRepository<ShipmentDocument, Long> {

	Page<ShipmentDocument> findByShipmentShipmentId(Long shipmentId, Pageable pageable);

	Page<ShipmentDocument> findByShipmentShipmentIdAndDocumentType(Long shipmentId,
			com.nexus.core.entities.ShipmentDocumentType documentType, Pageable pageable);
}