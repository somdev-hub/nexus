package com.nexus.core.repository;

import com.nexus.core.model.entities.ConsignmentConsumption;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsignmentConsumptionRepo extends JpaRepository<ConsignmentConsumption, Long> {
    Page<ConsignmentConsumption> findByRetailerOrgAccountId(Long orgId, Pageable pageable);
}