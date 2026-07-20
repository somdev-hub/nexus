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
public class ClientConfigResponse {

    private Long clientConfigId;
    private String clientName;
    private String connectionUrl;
    private String healthCheckPath;
    private Timestamp createdOn;
    private Timestamp updatedOn;
    private Boolean isActive;
    private List<ToolsConfigResponse> toolsConfigList;
}