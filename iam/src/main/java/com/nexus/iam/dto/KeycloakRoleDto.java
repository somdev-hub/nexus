package com.nexus.iam.dto;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Keycloak Role representation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeycloakRoleDto {
    private String id; // Keycloak UUID
    private String name;
    private String description;
    private boolean composite;
    private Set<String> attributes;
}

