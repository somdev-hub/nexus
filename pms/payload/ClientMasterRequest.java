package com.nexus.pms.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for creating/updating a client master record.
 * Clients are organizations like HR, Core, SUPPLY_CHAIN that use the PMS
 * platform.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientMasterRequest {

    /**
     * Unique identifier for the client (e.g., "HR", "CORE", "SUPPLY_CHAIN")
     */
    @NotBlank(message = "Client code is required")
    private String clientCode;

    /**
     * Display name of the client organization.
     */
    @NotBlank(message = "Client name is required")
    private String clientName;

    /**
     * Description of the client and their use case.
     */
    private String description;

    /**
     * Email contact for the client administrator.
     */
    @NotBlank(message = "Contact email is required")
    private String contactEmail;

    /**
     * Phone number for the client.
     */
    private String contactPhone;

    /**
     * Address of the client organization.
     */
    private String address;

    /**
     * City of the client.
     */
    private String city;

    /**
     * State/Province of the client.
     */
    private String state;

    /**
     * PIN/Postal code.
     */
    private String pinCode;

    /**
     * Country code.
     */
    private String country;

    /**
     * Whether this client is active or disabled.
     */
    @NotNull(message = "Active status is required")
    private Boolean isActive;

    /**
     * API key for authentication (optional field for configuration).
     */
    private String apiKey;

    /**
     * Webhook URL for payment notifications (optional).
     */
    private String webhookUrl;

    /**
     * Max amount allowed per transaction (optional).
     */
    private Double maxTransactionAmount;
}
