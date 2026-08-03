package com.nexus.nexusbuddy.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigInsightsResponse {
    private Map<String, Long> toolsPerClient;
    private Map<String, Long> paramsPerTool;
    private Map<String, Long> requiredVsOptionalParams;
    private Map<String, Long> dataTypeDistribution;
    private Map<String, Long> paramTypeDistribution;
    private List<String> clientsWithoutTools;
    private List<String> toolsWithoutParams;
}