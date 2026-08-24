package com.nexus.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.core.entities.Warehouse;

@Repository
public interface WarehouseRepo extends JpaRepository<Warehouse, Long> {

	Optional<List<Warehouse>> findByOrg(Long orgId);

	Optional<Warehouse> findByIdAndOrg(Long id, Long orgId);

}