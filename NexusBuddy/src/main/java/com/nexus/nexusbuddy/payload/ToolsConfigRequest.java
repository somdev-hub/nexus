package com.nexus.nexusbuddy.payload;

import com.nexus.nexusbuddy.model.enums.ToolsHttpMethod;
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
public class ToolsConfigRequest {

    @NotBlank(message = "Tool name is required")
    private String toolName;

    private String toolDescription;

    @NotBlank(message = "Endpoint is required")
    private String endpoint;

    @NotNull(message = "HTTP method is required")
    private ToolsHttpMethod httpMethod;

    @NotNull(message = "Active status is required")
    private Boolean isActive;

    private Long clientConfigId;

    private List<ToolsParamConfigRequest> paramConfigs;
}