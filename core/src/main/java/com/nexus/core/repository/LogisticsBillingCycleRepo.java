package com.nexus.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.core.model.entities.LogisticsBillingCycle;

@Repository
public interface LogisticsBillingCycleRepo extends JpaRepository<LogisticsBillingCycle, Long> {

	Optional<LogisticsBillingCycle> findByPartnershipIdAndCycleStart(Long partnershipId, java.sql.Timestamp cycleStart);

	List<LogisticsBillingCycle> findByPartnershipIdOrderByCycleStartAsc(Long partnershipId);
}
