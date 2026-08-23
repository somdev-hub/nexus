package com.nexus.nexusbuddy.payload;

import com.nexus.nexusbuddy.model.enums.DataType;
import com.nexus.nexusbuddy.model.enums.ParamType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolsParamConfigResponse {

    private Long toolsParamConfigId;
    private String paramName;
    private ParamType paramType;
    private DataType dataType;
    private Boolean isRequired;
    private Object defaultValue;
    private Object requestBodyJson;
    private String description;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Boolean isActive;
    private Long toolsConfigId;
}