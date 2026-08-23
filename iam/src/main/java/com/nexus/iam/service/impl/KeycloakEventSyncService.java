package com.nexus.iam.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.iam.dto.KeycloakRoleDto;
import com.nexus.iam.entities.Role;
import com.nexus.iam.repository.RoleRepository;
import com.nexus.iam.service.KeycloakAdminService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for periodically syncing roles from Keycloak to IAM database
 * 
 * This service polls Keycloak for new roles created via Keycloak Admin Console
 * and auto-syncs them to the IAM database, enabling seamless integration.
 * 
 * Runs on a configurable schedule (default: every 60 seconds).
 */
@Slf4j
@Service
public class KeycloakEventSyncService {

    @Autowired
    private KeycloakAdminService keycloakAdminService;

    @Autowired
    private RoleRepository roleRepository;

    /**
     * Scheduled task to sync roles from Keycloak to IAM database
     * Runs every 60 seconds by default
     * 
     * This allows roles created via Keycloak Admin Console to be automatically
     * discovered and synced to the IAM database for permission assignments.
     */
    @Scheduled(fixedDelayString = "${keycloak.role-sync-interval:60000}", initialDelayString = "${keycloak.role-sync-initial-delay:30000}")
    @Transactional
    @CircuitBreaker(name = "keycloak-admin", fallbackMethod = "syncRolesFallback")
    public void syncRolesFromKeycloak() {
        try {
            log.debug("Starting scheduled role sync from Keycloak...");

            // Fetch all roles from Keycloak
            List<KeycloakRoleDto> keycloakRoles = keycloakAdminService.listAllRoles();
            log.debug("Fetched {} roles from Keycloak", keycloakRoles.size());

            int newRolesCount = 0;
            int updatedRolesCount = 0;

            // Sync each role
            for (KeycloakRoleDto kcRole : keycloakRoles) {
                try {
                    var existingRole = roleRepository.findByName(kcRole.getName());

                    if (existingRole.isPresent()) {
                        // Role exists - update keycloak_id if missing
                        Role role = existingRole.get();
                        if (role.getKeycloakId() == null || role.getKeycloakId().isEmpty()) {
                            role.setKeycloakId(kcRole.getId());
                            roleRepository.save(role);
                            updatedRolesCount++;
                            log.debug("Updated role with Keycloak ID: {}", kcRole.getName());
                        }
                    } else {
                        // New role in Keycloak - create in IAM database
                        Role newRole = new Role();
                        newRole.setName(kcRole.getName());
                        newRole.setKeycloakId(kcRole.getId());
                        roleRepository.save(newRole);
                        newRolesCount++;
                        log.info("Synced new role from Keycloak: {}", kcRole.getName());
                    }
                } catch (Exception e) {
                    log.error("Error syncing role {}: {}", kcRole.getName(), e.getMessage());
                }
            }

            if (newRolesCount > 0 || updatedRolesCount > 0) {
                log.info("Keycloak role sync completed: {} new, {} updated",
                        newRolesCount, updatedRolesCount);
            }

        } catch (Exception e) {
            log.warn("Error during scheduled role sync: {}", e.getMessage());
            // Don't throw - allow next scheduled run to retry
        }
    }

    /**
     * Fallback method for circuit breaker
     * Logs warning and returns gracefully
     */
    public void syncRolesFallback(Exception e) {
        log.warn("Circuit breaker fallback: syncRolesFromKeycloak failed. Keycloak service may be unavailable. " +
                "Next sync attempt will run in 60 seconds.", e);
    }
}


