package com.nexus.core.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nexus.core.model.entities.CapacityReservation;

public interface CapacityReservationRepo extends JpaRepository<CapacityReservation, Long> {

	List<CapacityReservation> findBySourceTypeAndSourceIdAndIsActiveTrue(String sourceType, Long sourceId);
}
