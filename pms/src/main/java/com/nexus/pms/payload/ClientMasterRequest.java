package com.nexus.pms.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for registering a microservice client.
 * 
 * ClientMaster represents microservices using PMS:
 * - HR microservice (salary payments)
 * - CORE microservice (product payments)
 * - etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientMasterRequest {

    /**
     * Display name of the microservice/client.
     * Example: "Human Resources", "CORE System", "Supply Chain"
     */
    @NotBlank(message = "Client name is required")
    private String clientName;

    /**
     * Whether this client is active.
     */
    @NotNull(message = "Active status is required")
    private Boolean isActive;
}
