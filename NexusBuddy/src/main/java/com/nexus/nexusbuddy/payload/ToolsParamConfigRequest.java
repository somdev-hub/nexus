package com.nexus.nexusbuddy.payload;

import com.nexus.nexusbuddy.model.enums.DataType;
import com.nexus.nexusbuddy.model.enums.ParamType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolsParamConfigRequest {

    @NotBlank(message = "Parameter name is required")
    private String paramName;

    @NotNull(message = "Parameter type is required")
    private ParamType paramType;

    @NotNull(message = "Data type is required")
    private DataType dataType;

    @NotNull(message = "Required flag is required")
    private Boolean isRequired;

    private Object defaultValue;

    private Object requestBodyJson;

    private String description;

    @NotNull(message = "Active status is required")
    private Boolean isActive;

    private Long toolsConfigId;
}