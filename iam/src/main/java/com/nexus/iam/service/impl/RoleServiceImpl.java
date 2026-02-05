package com.nexus.iam.service.impl;

import com.nexus.iam.entities.Department;
import com.nexus.iam.entities.Role;
import com.nexus.iam.exception.ResourceNotFoundException;
import com.nexus.iam.exception.ServiceLevelException;
import com.nexus.iam.repository.DepartmentRepository;
import com.nexus.iam.repository.OrganizationRepository;
import com.nexus.iam.repository.RoleRepository;
import com.nexus.iam.repository.UserRepository;
import com.nexus.iam.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    public void initializeRoles() {
        try {
            String[] roleNames = {"ADMIN", "DIRECTOR", "PRODUCT_MANAGER", "CLERK",
                    "ACCOUNT_MANAGER", "OPERATION_MANAGER", "WAREHOUSE_MANAGER",
                    "FLEET_MANAGER", "DRIVER"};

            for (String roleName : roleNames) {
                if (!roleRepository.existsByName(roleName)) {
                    Role role = new Role();
                    role.setName(roleName);
                    roleRepository.save(role);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error initializing roles: " + e.getMessage(), e);
        }
    }

    @Override
    public ResponseEntity<?> createRoleIfNotFound(String roleName, Long deptId) {
        if (ObjectUtils.isEmpty(roleName) || ObjectUtils.isEmpty(deptId)) {
            throw new IllegalArgumentException("Role name, Department ID, and Organization ID cannot be null or empty");
        }

        try {
            // Fetch the department
            Department department = departmentRepository.findById(deptId)
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", deptId));

            // Check if role exists
            if (roleRepository.existsByName(roleName)) {
                // Role exists - check if it's already in the department
                Role existingRole = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));

                if (department.getRoles().contains(existingRole)) {
                    // Role already exists in this department
                    throw new ServiceLevelException(
                            "RoleServiceImpl",
                            "Role '" + roleName + "' already exists in department '" + department.getDepartmentName() + "'",
                            "createRoleIfNotFound",
                            "RoleAlreadyExistsException",
                            "Cannot add duplicate role to department"
                    );
                } else {
                    // Role exists but not in this department - add it
                    department.getRoles().add(existingRole);
                    departmentRepository.save(department);
                    return ResponseEntity.ok("Role '" + roleName + "' added to department successfully");
                }
            } else {
                // Role doesn't exist - create new role and add to department
                Role newRole = new Role();
                newRole.setName(roleName);
                Role savedRole = roleRepository.save(newRole);

                department.getRoles().add(savedRole);
                departmentRepository.save(department);
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body("Role '" + roleName + "' created and added to department successfully");
            }

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RoleServiceImpl",
                    "Failed to create role: " + roleName,
                    "createRoleIfNotFound",
                    e.getClass().getSimpleName(),
                    e.getLocalizedMessage()
            );
        }
    }

    @Override
    public void deleteRoleByName(String roleName) {
        roleRepository.findByName(roleName).ifPresent(roleRepository::delete);
    }

}
