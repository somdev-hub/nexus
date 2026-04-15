package com.nexus.iam.repository;

import com.nexus.iam.entities.Department;
import com.nexus.iam.entities.Organization;
import com.nexus.iam.entities.Role;
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

    @Query(value = "SELECT DISTINCT u.* FROM iam.t_users u LEFT JOIN iam.t_department_members dm ON u.id = dm.user_id LEFT JOIN iam.t_departments d ON (d.department_id = dm.department_id OR d.department_head_id = u.id) WHERE dm.department_id = :departmentId OR d.department_id = :departmentId;", countQuery =
            "SELECT COUNT(DISTINCT u.id) FROM iam.t_users u LEFT JOIN iam.t_department_members dm ON u.id = dm.user_id LEFT JOIN iam.t_departments d ON (d.department_id = dm.department_id OR d.department_head_id = u.id) WHERE dm.department_id = :departmentId OR d.department_id = :departmentId;"
            , nativeQuery = true)
    Page<User> findByDepartmentId(Long departmentId, Pageable pageable);

    Page<User> findByOrganization(Organization organization, Pageable pageable);

    @Query("""
                                    SELECT u FROM User u
                                    WHERE u.organization.id = :organizationId
            """)
    Page<User> findByOrganizationIdWithPagination(Long organizationId, Pageable pageable);

    @Query("""
                                                SELECT DISTINCT u FROM User u
                                                JOIN u.memberOfDepartments d
                                                JOIN u.headedDepartments h
                                                JOIN u.roles r
                                                WHERE (d = :department OR h=:department) AND r = :role
            """)
    Page<User> findByDepartmentAndRole(Department department, Role role, Pageable pageable);
}
