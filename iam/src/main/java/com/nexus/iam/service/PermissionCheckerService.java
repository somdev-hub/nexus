package com.nexus.iam.service;

import com.nexus.iam.entities.Permission;
import com.nexus.iam.entities.User;
import com.nexus.iam.entities.Role;
import com.nexus.iam.entities.Department;
import com.nexus.iam.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionCheckerService {

    private final PermissionRepository permissionRepository;

    /**
     * Check if user has permission for a specific resource and action
     */
    public boolean hasPermission(User user, String resourceName, String action) {
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return false;
        }

        return user.getRoles().stream()
                .flatMap(role -> permissionRepository.findByRole(role).stream())
                .anyMatch(permission -> permission.getResource() != null &&
                        permission.getResource().getResourceName().equals(resourceName) &&
                        permission.getAction() != null &&
                        permission.getAction().name().equals(action));
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
                        permission.getAction() != null &&
                        permission.getAction().name().equals(action) &&
                        (permission.getDepartment() == null ||
                                (permission.getDepartment() != null && permission.getDepartment().equals(department))));
    }

    /**
     * Check if role has permission for a resource and action
     */
    public boolean roleHasPermission(Role role, String resourceName, String action) {
        if (role == null) {
            return false;
        }

        return permissionRepository.findByRole(role).stream()
                .anyMatch(permission -> permission.getResource() != null &&
                        permission.getResource().getResourceName().equals(resourceName) &&
                        permission.getAction() != null &&
                        permission.getAction().name().equals(action));
    }

    /**
     * Get all allowed actions for a user on a specific resource
     */
    public Set<String> getAllowedActions(User user, String resourceName) {
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return Set.of();
        }

        return user.getRoles().stream()
                .flatMap(role -> permissionRepository.findByRole(role).stream())
                .filter(permission -> permission.getResource() != null &&
                        permission.getResource().getResourceName().equals(resourceName))
                .map(permission -> permission.getAction().name())
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
                .map(permission -> permission.getAction().name())
                .collect(Collectors.toSet());
    }
}
