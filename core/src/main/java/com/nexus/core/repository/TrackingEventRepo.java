package com.nexus.core.repository;

import java.sql.Timestamp;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nexus.core.entities.TrackingEvent;
import com.nexus.core.entities.TrackingEventType;

@Repository
public interface TrackingEventRepo extends JpaRepository<TrackingEvent, Long> {

	Page<TrackingEvent> findByShipmentShipmentId(Long shipmentId, Pageable pageable);

	Page<TrackingEvent> findByShipmentShipmentIdAndEventType(Long shipmentId, TrackingEventType eventType,
			Pageable pageable);

	@Query("SELECT te FROM TrackingEvent te WHERE te.shipment.shipmentId = :shipmentId ORDER BY te.eventTimestamp DESC")
	Page<TrackingEvent> findByShipmentOrderByTimestampDesc(@Param("shipmentId") Long shipmentId, Pageable pageable);

	@Query("SELECT te FROM TrackingEvent te WHERE te.shipment.shipmentId = :shipmentId AND te.eventTimestamp BETWEEN :start AND :end ORDER BY te.eventTimestamp")
	Page<TrackingEvent> findByShipmentAndTimestampBetween(@Param("shipmentId") Long shipmentId,
			@Param("start") Timestamp start, @Param("end") Timestamp end, Pageable pageable);

	TrackingEvent findFirstByShipmentShipmentIdOrderByEventTimestampDesc(Long shipmentId);
}