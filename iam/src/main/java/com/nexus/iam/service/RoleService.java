package com.nexus.iam.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public interface RoleService {
    public void initializeRoles();

    public ResponseEntity<?> createRoleIfNotFound(String roleName, Long deptId);

    public void deleteRoleByName(String roleName);

}
