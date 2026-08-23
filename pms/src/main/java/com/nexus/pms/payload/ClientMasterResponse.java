package com.nexus.pms.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * DTO for client master response.
 * Returns client configuration and metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientMasterResponse {

    /**
     * Unique database ID of the client.
     */
    private Long clientMasterId;

    /**
     * Client code (e.g., "HR", "CORE").
     */
    private String clientCode;

    /**
     * Client organization name.
     */
    private String clientName;

    /**
     * Description.
     */
    private String description;

    /**
     * Contact email.
     */
    private String contactEmail;

    /**
     * Contact phone.
     */
    private String contactPhone;

    /**
     * Full address.
     */
    private String address;

    /**
     * City.
     */
    private String city;

    /**
     * State.
     */
    private String state;

    /**
     * PIN code.
     */
    private String pinCode;

    /**
     * Country.
     */
    private String country;

    /**
     * Active status.
     */
    private Boolean isActive;

    /**
     * Webhook URL for this client.
     */
    private String webhookUrl;

    /**
     * Max transaction amount allowed.
     */
    private Double maxTransactionAmount;

    /**
     * When the client was created.
     */
    private Timestamp createdAt;

    /**
     * When the client was last updated.
     */
    private Timestamp updatedAt;

    /**
     * Success flag.
     */
    private Boolean success;

    /**
     * Error message if applicable.
     */
    private String errorMessage;
}
