package com.nexus.core.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.nexus.core.model.entities.Material;
public interface MaterialRepo extends JpaRepository<Material, Long> {

	Page<Material> findByOrg(Long orgId, Pageable pageable);

	Optional<Material> findByMaterialIdAndOrg(Long materialId, Long orgId);

}
