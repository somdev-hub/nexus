package com.nexus.iam.service.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.nexus.iam.dto.GrantPermissionDto;
import com.nexus.iam.entities.Department;
import com.nexus.iam.entities.Permission;
import com.nexus.iam.entities.PermissionAction;
import com.nexus.iam.entities.Resource;
import com.nexus.iam.entities.Role;
import com.nexus.iam.entities.User;
import com.nexus.iam.exception.ResourceNotFoundException;
import com.nexus.iam.repository.DepartmentRepository;
import com.nexus.iam.repository.PermissionRepository;
import com.nexus.iam.repository.ResourceRepository;
import com.nexus.iam.repository.RoleRepository;
import com.nexus.iam.repository.UserRepository;
import com.nexus.iam.service.PermissionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final ResourceRepository resourceRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    @Override
    public ResponseEntity<?> grantPermission(GrantPermissionDto grantPermissionDto) {
        try {
            if (ObjectUtils.isEmpty(grantPermissionDto.getRole()) ||
                    ObjectUtils.isEmpty(grantPermissionDto.getResourceName()) ||
                    ObjectUtils.isEmpty(grantPermissionDto.getActions())) {
                return ResponseEntity.badRequest()
                        .body("Role, Resource, and Action cannot be null");
            }

            if (grantPermissionDto.getRole().startsWith("ROLE_")) {
                grantPermissionDto.setRole(grantPermissionDto.getRole().substring(5)); // Remove "ROLE_" prefix if present
            }

            // Fetch role
            Role role = roleRepository.findByName(grantPermissionDto.getRole())
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "name", grantPermissionDto.getRole()));

            // Fetch or create resource
            Resource resource = resourceRepository.findByResourceName(grantPermissionDto.getResourceName())
                    .orElseGet(() -> {
                        Resource newResource = new Resource();
                        newResource.setResourceName(grantPermissionDto.getResourceName());
                        newResource.setDescription(grantPermissionDto.getDescription());
                        newResource.setResourceType(grantPermissionDto.getResourceType());
                        if (!ObjectUtils.isEmpty(grantPermissionDto.getResourceUrl())){
                            newResource.setResourceUrl(grantPermissionDto.getResourceUrl());
                        }
                        if (!ObjectUtils.isEmpty(grantPermissionDto.getFeatureId())){
                            newResource.setFeatureId(grantPermissionDto.getFeatureId());
                        }
                        return resourceRepository.save(newResource);
                    });

            // Fetch department if provided
            Department department = null;
            if (!ObjectUtils.isEmpty(grantPermissionDto.getDepartmentId())) {
                department = departmentRepository.findById(grantPermissionDto.getDepartmentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Department", "id",
                                grantPermissionDto.getDepartmentId()));
            }

            // Check if permission already exists
            Set<PermissionAction> actions = grantPermissionDto.getActions();
            Permission existingPermission = permissionRepository.findByRoleAndResourceAndDepartment(role, resource, department)
                    .orElse(null);

            if (existingPermission != null) {
                // Check if all actions already exist
                if (existingPermission.getActions().containsAll(actions)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body("All requested permissions already exist for this role and resource");
                }

                // Add new actions to existing permission
                existingPermission.getActions().addAll(actions);
                Permission savedPermission = permissionRepository.save(existingPermission);
                Map<String,String> response = new HashMap<>();
                response.put("message", "Permission updated successfully with new actions");
                response.put("permissionId", savedPermission.getPermissionId().toString());
                response.put("status", "200");
                return ResponseEntity.status(HttpStatus.OK).body(response);
            }

            // Create and save permission
            Permission permission = new Permission();
            permission.setRole(role);
            permission.setResource(resource);
            permission.setActions(actions);
            permission.setDepartment(department);

            Permission savedPermission = permissionRepository.save(permission);
            Map<String,String> response = new HashMap<>();
            response.put("message", "Permission granted successfully");
            response.put("permissionId", savedPermission.getPermissionId().toString());
            response.put("status", "201");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error granting permission: " + ex.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> revokePermission(Long permissionId) {
        try {
            if (ObjectUtils.isEmpty(permissionId)) {
                return ResponseEntity.badRequest().body("Permission ID cannot be null");
            }

            Permission permission = permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", permissionId));

            permissionRepository.delete(permission);
            return ResponseEntity.ok("Permission revoked successfully");
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error revoking permission: " + ex.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> hasPermission(GrantPermissionDto grantPermissionDto) {
        try {
            if (ObjectUtils.isEmpty(grantPermissionDto.getRole()) ||
                    ObjectUtils.isEmpty(grantPermissionDto.getResourceName()) ||
                    ObjectUtils.isEmpty(grantPermissionDto.getActions())) {
                return ResponseEntity.badRequest()
                        .body("Role, Resource, and Action cannot be null");
            }

            Role role = roleRepository.findByName(grantPermissionDto.getRole())
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "name", grantPermissionDto.getRole()));

            Resource resource = resourceRepository.findByResourceName(grantPermissionDto.getResourceName())
                    .orElseThrow(() -> new ResourceNotFoundException("Resource", "name",
                            grantPermissionDto.getResourceName()));

            Department department = null;
            if (!ObjectUtils.isEmpty(grantPermissionDto.getDepartmentId())) {
                department = departmentRepository.findById(grantPermissionDto.getDepartmentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Department", "id",
                                grantPermissionDto.getDepartmentId()));
            }

            boolean hasPermission = permissionRepository
                    .findByRoleAndResourceAndDepartment(role, resource, department)
                    .map(permission -> permission.getActions().containsAll(grantPermissionDto.getActions()))
                    .orElse(false);
            return ResponseEntity.ok(hasPermission);
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error checking permission: " + ex.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> roleHasPermission(GrantPermissionDto grantPermissionDto) {
        try {
            if (ObjectUtils.isEmpty(grantPermissionDto.getRole()) ||
                    ObjectUtils.isEmpty(grantPermissionDto.getResourceName()) ||
                    ObjectUtils.isEmpty(grantPermissionDto.getActions())) {
                return ResponseEntity.badRequest()
                        .body("Role, Resource, and Action cannot be null");
            }

            Role role = roleRepository.findByName(grantPermissionDto.getRole())
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "name", grantPermissionDto.getRole()));

            boolean hasPermission = roleHasPermissionCheck(role,
                    grantPermissionDto.getResourceName(), grantPermissionDto.getActions());
            return ResponseEntity.ok(hasPermission);
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error checking role permission: " + ex.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getAllowedActions(Long userId, String resourceName) {
        try {
            if (ObjectUtils.isEmpty(userId) || ObjectUtils.isEmpty(resourceName)) {
                return ResponseEntity.badRequest()
                        .body("User ID and Resource Name cannot be null");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

            Set<String> allowedActions = getPermissionCheckerAllowedActions(user, resourceName);
            return ResponseEntity.ok(allowedActions);
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving allowed actions: " + ex.getMessage());
        }
    }

    // Permission Checker Methods

    /**
     * Check if user has permission for a specific resource and action
     */
    public boolean hasPermissionCheck(User user, String resourceName, String action) {
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return false;
        }

        return user.getRoles().stream()
                .flatMap(role -> permissionRepository.findByRole(role).stream())
                .anyMatch(permission -> permission.getResource() != null &&
                        permission.getResource().getResourceName().equals(resourceName) &&
                        permission.getActions() != null &&
                        permission.getActions().stream()
                            .anyMatch(action_enum -> action_enum.name().equals(action)));
    }

    /**
     * Check if user has department-specific permission for a resource and action
     */
    public boolean hasPermissionForDepartment(User user, String resourceName, String action, Department department) {
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return false;
        }

        return user.getRoles().stream()
                .flatMap(role -> permissionRepository.findByRole(role).stream())
                .anyMatch(permission -> permission.getResource() != null &&
                        permission.getResource().getResourceName().equals(resourceName) &&
                        permission.getActions() != null &&
                        permission.getActions().stream()
                            .anyMatch(action_enum -> action_enum.name().equals(action)) &&
                        (permission.getDepartment() == null ||
                                permission.getDepartment().equals(department)));
    }

    /**
     * Check if role has permission for a resource and action
     */
    public boolean roleHasPermissionCheck(Role role, String resourceName, String action) {
        if (role == null) {
            return false;
        }

        return permissionRepository.findByRole(role).stream()
                .anyMatch(permission -> permission.getResource() != null &&
                        permission.getResource().getResourceName().equals(resourceName) &&
                        permission.getActions() != null &&
                        permission.getActions().stream()
                            .anyMatch(action_enum -> action_enum.name().equals(action)));
    }

    /**
     * Check if role has permission for a resource and set of actions
     */
    public boolean roleHasPermissionCheck(Role role, String resourceName, Set<PermissionAction> requiredActions) {
        if (role == null) {
            return false;
        }

        return permissionRepository.findByRole(role).stream()
                .anyMatch(permission -> permission.getResource() != null &&
                        permission.getResource().getResourceName().equals(resourceName) &&
                        permission.getActions() != null &&
                        permission.getActions().containsAll(requiredActions));
    }

    /**
     * Get all allowed actions for a user on a specific resource
     */
    public Set<String> getPermissionCheckerAllowedActions(User user, String resourceName) {
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return Set.of();
        }

        return user.getRoles().stream()
                .flatMap(role -> permissionRepository.findByRole(role).stream())
                .filter(permission -> permission.getResource() != null &&
                        permission.getResource().getResourceName().equals(resourceName))
                .flatMap(permission -> permission.getActions().stream())
                .map(PermissionAction::name)
                .collect(Collectors.toSet());
    }

    /**
     * Get all allowed actions for a role on a specific resource
     */
    public Set<String> getRoleAllowedActions(Role role, String resourceName) {
        if (role == null) {
            return Set.of();
        }

        return permissionRepository.findByRole(role).stream()
                .filter(permission -> permission.getResource() != null &&
                        permission.getResource().getResourceName().equals(resourceName))
                .flatMap(permission -> permission.getActions().stream())
                .map(PermissionAction::name)
                .collect(Collectors.toSet());
    }
}
