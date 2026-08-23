package com.nexus.nexusbuddy.payload;

import com.nexus.nexusbuddy.model.enums.ToolsHttpMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolsConfigResponse {

    private Long toolsConfigId;
    private String toolName;
    private String toolDescription;
    private String endpoint;
    private ToolsHttpMethod httpMethod;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Boolean isActive;
    private Long clientConfigId;
    private List<ToolsParamConfigResponse> paramConfigs;
}