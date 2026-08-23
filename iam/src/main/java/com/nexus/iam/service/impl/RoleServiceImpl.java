package com.nexus.iam.service.impl;

import com.nexus.iam.dto.KeycloakRoleDto;
import com.nexus.iam.entities.Department;
import com.nexus.iam.entities.Role;
import com.nexus.iam.exception.ResourceNotFoundException;
import com.nexus.iam.exception.ServiceLevelException;
import com.nexus.iam.repository.DepartmentRepository;
import com.nexus.iam.repository.OrganizationRepository;
import com.nexus.iam.repository.RoleRepository;
import com.nexus.iam.repository.UserRepository;
import com.nexus.iam.security.JwtUtil;
import com.nexus.iam.service.KeycloakAdminService;
import com.nexus.iam.service.RoleService;
import com.nexus.iam.utils.WebConstants;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final JwtUtil jwtUtil;
    private final KeycloakAdminService keycloakAdminService;
    private final WebConstants webConstants;

    @Override
    public void initializeRoles() {
        try {
            String[] roleNames = { "ADMIN", "DIRECTOR", "PRODUCT_MANAGER", "CLERK",
                    "ACCOUNT_MANAGER", "OPERATION_MANAGER", "WAREHOUSE_MANAGER",
                    "FLEET_MANAGER", "DRIVER" };

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
    public ResponseEntity<?> createRoleIfNotFound(String roleName, Long deptId, String authHeader) {
        if (ObjectUtils.isEmpty(roleName) || ObjectUtils.isEmpty(deptId)) {
            throw new IllegalArgumentException("Role name, Department ID, and Organization ID cannot be null or empty");
        }

        try {
            // Extract username from token (handles both Keycloak and traditional JWT)
            String cleanToken = authHeader;
            if (authHeader.startsWith("Bearer ")) {
                cleanToken = authHeader.substring(7);
            }
            String subject = jwtUtil.extractUsernameFromToken(cleanToken);

            if (subject == null) {
                return ResponseEntity.badRequest().body("Invalid token: unable to extract user information");
            }

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
                            "Role '" + roleName + "' already exists in department '" + department.getDepartmentName()
                                    + "'",
                            "createRoleIfNotFound",
                            "RoleAlreadyExistsException",
                            "Cannot add duplicate role to department");
                } else {
                    // Role exists but not in this department - add it
                    department.getRoles().add(existingRole);
                    departmentRepository.save(department);
                    return ResponseEntity.ok("Role '" + roleName + "' added to department successfully");
                }
            } else {
                // Role doesn't exist - create new role and add to department
                Role newRole = new Role();
                newRole.setName(roleName.toUpperCase().replaceAll("\\s+", "_"));
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
                    e.getLocalizedMessage());
        }
    }

    @Override
    public void deleteRoleByName(String roleName) {
        roleRepository.findByName(roleName).ifPresent(roleRepository::delete);
    }

    @Override
    public void syncKeycloakIds() {
        try {
            log.info("Starting Keycloak ID sync for roles");

            // Get all roles from the database that have null keycloak_id
            List<Role> rolesToSync = roleRepository.findAll()
                    .stream()
                    .filter(role -> role.getKeycloakId() == null)
                    .toList();

            if (rolesToSync.isEmpty()) {
                log.info("All roles already have keycloak_id set. No sync needed.");
                return;
            }

            log.info("Found {} roles to sync with Keycloak", rolesToSync.size());

            // Try to fetch roles from Keycloak
            List<KeycloakRoleDto> keycloakRoles = null;
            try {
                keycloakRoles = keycloakAdminService.listAllRoles();
                log.debug("Retrieved {} roles from Keycloak", keycloakRoles.size());
            } catch (RuntimeException e) {
                log.warn("Failed to fetch roles from Keycloak: {}. Reason: {}",
                        e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "Unknown");
                log.warn("Keycloak sync skipped. Ensure:");
                log.warn("  1. Keycloak server is running at: {}", webConstants.getKeycloakServerUrl());
                log.warn("  2. Admin client credentials are correct (client_id: {})",
                        webConstants.getKeycloakAdminClientId());
                log.warn("  3. Admin client has 'admin' role in realm");
                log.warn("  4. Roles in Keycloak match role names in IAM database");
                log.warn("Skipping Keycloak sync. You can retry later when Keycloak is available.");
                return;
            }

            if (keycloakRoles == null || keycloakRoles.isEmpty()) {
                log.warn("No roles found in Keycloak. Skipping sync.");
                return;
            }

            // Sync keycloak_id for each role
            int syncedCount = 0;
            int notFoundCount = 0;
            for (Role iamRole : rolesToSync) {
                // Find matching role in Keycloak by name
                KeycloakRoleDto keycloakRole = keycloakRoles.stream()
                        .filter(kr -> kr.getName() != null && kr.getName().equalsIgnoreCase(iamRole.getName()))
                        .findFirst()
                        .orElse(null);

                if (keycloakRole != null && keycloakRole.getId() != null) {
                    // Update the role with keycloak_id
                    iamRole.setKeycloakId(keycloakRole.getId());
                    roleRepository.save(iamRole);
                    syncedCount++;
                    log.debug("Synced role: {} with keycloak_id: {}", iamRole.getName(), keycloakRole.getId());
                } else {
                    notFoundCount++;
                    log.warn(
                            "Role '{}' not found in Keycloak. Create it in Keycloak or update role name in IAM to match.",
                            iamRole.getName());
                }
            }

            log.info("Keycloak ID sync completed. {} roles synced successfully, {} roles not found in Keycloak",
                    syncedCount, notFoundCount);

        } catch (Exception e) {
            log.error("Unexpected error during Keycloak ID sync: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to sync Keycloak IDs: " + e.getMessage(), e);
        }
    }

}
