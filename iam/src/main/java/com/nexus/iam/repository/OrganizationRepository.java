package com.nexus.iam.repository;

import com.nexus.iam.entities.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByOrgName(String orgName);

    Boolean existsByOrgName(String orgName);
}
