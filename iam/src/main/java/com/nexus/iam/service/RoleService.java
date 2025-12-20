package com.nexus.iam.service;

import org.springframework.stereotype.Service;

@Service
public interface RoleService {
    public void initializeRoles();

    public void createRoleIfNotFound(String roleName);

    public void deleteRoleByName(String roleName);

}
