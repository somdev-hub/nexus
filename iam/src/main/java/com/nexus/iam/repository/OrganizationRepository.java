package com.nexus.iam.repository;

import com.nexus.iam.dto.OrganizationFetchDto;
import com.nexus.iam.entities.Organization;
import com.nexus.iam.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByOrgName(String orgName);

    Boolean existsByOrgName(String orgName);

    @Query("SELECT new com.nexus.iam.dto.OrganizationFetchDto(o.id, o.orgName, o.orgType, o.trustScore, o.createdAt, " +
            "COUNT(u)) FROM Organization o LEFT JOIN o.users u WHERE o.id = :orgId GROUP BY o.id, o.orgName, o.orgType, o.trustScore, o.createdAt")
    Optional<OrganizationFetchDto> fetchByOrgId(Long orgId);

    @Query("""
            SELECT DISTINCT u FROM User u
            WHERE u.organization.id = :orgId
              AND u.id IN (
                SELECT dm.id FROM Department d
                JOIN d.members dm
                WHERE d.departmentId = :deptId
              )
              AND u.id IN (
                SELECT ur.id FROM User ur
                JOIN ur.roles r
                WHERE r.name = :role
              )
            """)
    Page<User> findByOrgIdAndDeptIdAndRoleWithPagination(@Param("orgId") Long orgId,
                                                         @Param("deptId") Long deptId,
                                                         @Param("role") String role,
                                                         Pageable pageable);

    @Query("""
            SELECT DISTINCT u FROM User u
            WHERE u.organization.id = :orgId
              AND u.id IN (
                SELECT dm.id FROM Department d
                JOIN d.members dm
                WHERE d.departmentId = :deptId
              )
            """)
    Page<User> findByOrgIdAndDeptIdWithPagination(@Param("orgId") Long orgId,
                                                  @Param("deptId") Long deptId, Pageable pageable);

    @Query("""
            SELECT DISTINCT u FROM User u
            WHERE u.organization.id = :orgId
              AND u.id IN (
                SELECT ur.id FROM User ur
                JOIN ur.roles r
                WHERE r.name = :role
              )
            """)
    Page<User> findByOrgIdAndRoleWithPagination(@Param("orgId") Long orgId,
                                                @Param("role") String role, Pageable pageable);
}
