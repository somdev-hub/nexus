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
public class PerformanceResponse {
    private Map<String, Double> latencyPercentiles;
    private List<TimeSeriesDataPoint> errorRateTrend;
    private Double availabilityPercentage;
    private List<ToolPerformanceDataPoint> toolSuccessRates;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesDataPoint {
        private String timestamp;
        private Double value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolPerformanceDataPoint {
        private String toolName;
        private Long requestCount;
        private Double successRate;
        private Double avgLatencyMs;
    }
}
