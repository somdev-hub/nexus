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
public class ToolUsageResponse {
    private Map<String, Long> httpMethodDistribution;
    private List<ToolUsageDataPoint> topToolsByVolume;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolUsageDataPoint {
        private String toolName;
        private Long requestCount;
        private Double successRate;
    }
}
