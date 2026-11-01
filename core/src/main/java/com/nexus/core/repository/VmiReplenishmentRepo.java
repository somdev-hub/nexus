package com.nexus.core.repository;

import com.nexus.core.model.entities.VmiReplenishment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VmiReplenishmentRepo extends JpaRepository<VmiReplenishment, Long> {
    Page<VmiReplenishment> findByRetailerOrgAccountId(Long orgId, Pageable pageable);
}