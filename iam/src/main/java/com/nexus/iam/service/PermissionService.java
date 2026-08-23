package com.nexus.iam.service;

import org.springframework.http.ResponseEntity;

import com.nexus.iam.dto.GrantPermissionDto;

public interface PermissionService {
    ResponseEntity<?> grantPermission(GrantPermissionDto grantPermissionDto);

    ResponseEntity<?> revokePermission(Long permissionId);

    ResponseEntity<?> hasPermission(GrantPermissionDto grantPermissionDto);

    ResponseEntity<?> roleHasPermission(GrantPermissionDto grantPermissionDto);

    ResponseEntity<?> getAllowedActions(Long userId, String resourceName);
}
