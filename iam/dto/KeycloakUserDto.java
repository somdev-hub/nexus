package com.nexus.iam.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Keycloak User representation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeycloakUserDto {
    private String id; // Keycloak UUID
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private Boolean enabled;
    private Boolean emailVerified;
    private String password;
    private Map<String, Object> attributes;
}

