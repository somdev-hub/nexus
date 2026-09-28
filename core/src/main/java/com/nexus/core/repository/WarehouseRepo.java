package com.nexus.core.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.nexus.core.model.entities.Warehouse;
public interface WarehouseRepo extends JpaRepository<Warehouse, Long> {

	Page<Warehouse> findByOrg(Long orgId, Pageable pageable);

	Optional<Warehouse> findByWarehouseIdAndOrg(Long warehouseId, Long orgId);

}