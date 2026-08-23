package com.nexus.nexusbuddy.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientConfigRequest {

    @NotBlank(message = "Client name is required")
    private String clientName;

    private String connectionUrl;

    private String healthCheckPath;

    private List<String> allowedUsersList;

    @NotNull(message = "Active status is required")
    private Boolean isActive;
}