package com.nexus.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.core.entities.Material;

@Repository
public interface MaterialRepo extends JpaRepository<Material, Long> {

	Optional<List<Material>> findByOrg(Long orgId);

	Optional<Material> findByIdAndOrg(Long id, Long orgId);

}
