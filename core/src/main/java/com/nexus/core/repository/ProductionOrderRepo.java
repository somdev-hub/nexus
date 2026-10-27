package com.nexus.core.repository;

import com.nexus.core.model.entities.ProductionOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionOrderRepo extends JpaRepository<ProductionOrder, Long> {
	Page<ProductionOrder> findByOrg(Long orgId, Pageable pageable);
}