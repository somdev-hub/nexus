package com.nexus.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.core.model.entities.LogisticsPayableLine;
import com.nexus.core.model.enums.LogisticsPayableLineKind;
import com.nexus.core.model.enums.LogisticsPayableLineStatus;

@Repository
public interface LogisticsPayableLineRepo extends JpaRepository<LogisticsPayableLine, Long> {

	Optional<LogisticsPayableLine> findByPartnershipIdAndShipmentIdAndKind(Long partnershipId, Long shipmentId,
			LogisticsPayableLineKind kind);

	List<LogisticsPayableLine> findByPartnershipIdAndStatusInOrderByDueDateAsc(Long partnershipId,
			List<LogisticsPayableLineStatus> statuses);

	List<LogisticsPayableLine> findByPartnershipIdAndCycleIdOrderByDueDateAsc(Long partnershipId, String cycleId);

	List<LogisticsPayableLine> findByShipmentId(Long shipmentId);

	List<LogisticsPayableLine> findByPartnershipId(Long partnershipId);
}
