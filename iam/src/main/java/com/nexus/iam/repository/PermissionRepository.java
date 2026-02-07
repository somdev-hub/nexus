package com.nexus.iam.repository;

import com.nexus.iam.entities.Permission;
import com.nexus.iam.entities.Role;
import com.nexus.iam.entities.Resource;
import com.nexus.iam.entities.Department;
import com.nexus.iam.entities.PermissionAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    List<Permission> findByRole(Role role);

    List<Permission> findByRoleAndDepartment(Role role, Department department);

    List<Permission> findByRoleAndAction(Role role, PermissionAction action);

    Optional<Permission> findByRoleAndResourceAndAction(Role role, Resource resource, PermissionAction action);

    List<Permission> findByResource(Resource resource);

    List<Permission> findByDepartment(Department department);

    @Query("SELECT p FROM Permission p WHERE p.role = :role AND p.resource = :resource AND p.action = :action AND (p.department = :department OR p.department IS NULL)")
    Optional<Permission> findPermission(Role role, Resource resource, PermissionAction action, Department department);
}
