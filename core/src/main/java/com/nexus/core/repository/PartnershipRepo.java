package com.nexus.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.core.entities.Partnership;

@Repository
public interface PartnershipRepo extends JpaRepository<Partnership, Long> {

	Optional<List<Partnership>> findByPrimaryOrg(Long orgId);

	Optional<Partnership> findByIdAndPrimaryOrg(Long id, Long orgId);

}