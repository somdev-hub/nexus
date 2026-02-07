package com.nexus.iam.repository;

import com.nexus.iam.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByName(String name);

    Optional<User> findByEmail(String email);

    Boolean existsByName(String name);

    Boolean existsByEmail(String email);

    @Query(value = "SELECT u.* " +
            "FROM iam.t_users u " +
            "WHERE u.organization_id = :orgId", countQuery = "SELECT count(u.id) FROM iam.t_users u " +
            "WHERE u.organization_id = :orgId", nativeQuery = true)
    Page<User> findByOrgId(Long orgId, Pageable pageable);

    Boolean existsByEmailAndOrganizationId(String name, Long orgId);

    @Query(value = "SELECT CASE WHEN COUNT(u.id) > 0 THEN true ELSE false END " +
            "FROM iam.t_users u " +
            "INNER JOIN iam.t_organizations dm ON u.organization_id=dm.id " +
            "INNER JOIN iam.t_departments d ON d.org_id=dm.id " +
            "WHERE u.email = :email " +
            "AND d.department_id = :departmentId " +
            "AND u.organization_id = d.org_id", nativeQuery = true)
    Boolean existsByEmailAndDepartmentId(String email, Long departmentId);
}
