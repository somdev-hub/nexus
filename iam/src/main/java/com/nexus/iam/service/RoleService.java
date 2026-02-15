package com.nexus.iam.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

public interface RoleService {
    void initializeRoles();

    ResponseEntity<?> createRoleIfNotFound(String roleName, Long deptId, String authHeader);

    void deleteRoleByName(String roleName);

}
