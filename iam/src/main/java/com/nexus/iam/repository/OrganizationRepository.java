package com.nexus.iam.repository;

import com.nexus.iam.dto.OrganizationFetchDto;
import com.nexus.iam.entities.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByOrgName(String orgName);

    Boolean existsByOrgName(String orgName);

    @Query("SELECT new com.nexus.iam.dto.OrganizationFetchDto(o.id, o.orgName, o.orgType, o.trustScore, o.createdAt, " +
            "COUNT(u)) FROM Organization o LEFT JOIN o.users u WHERE o.id = :orgId GROUP BY o.id, o.orgName, o.orgType, o.trustScore, o.createdAt")
    Optional<OrganizationFetchDto> fetchByOrgId(Long orgId);
}
