package com.nexus.iam.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nexus.iam.entities.Role;
import com.nexus.iam.repository.RoleRepository;
import com.nexus.iam.service.RoleService;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleRepository roleRepository;

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
    public void createRoleIfNotFound(String roleName) {
        if (!roleRepository.existsByName(roleName)) {
            Role role = new Role();
            role.setName(roleName);
            roleRepository.save(role);
        }
    }

    @Override
    public void deleteRoleByName(String roleName) {
        roleRepository.findByName(roleName).ifPresent(roleRepository::delete);
    }

}
