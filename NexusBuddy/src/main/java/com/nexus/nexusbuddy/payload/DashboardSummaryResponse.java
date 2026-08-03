package com.nexus.nexusbuddy.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private Long totalClients;
    private Long activeClients;
    private Long totalTools;
    private Long activeTools;
    private Long requestsLast24h;
    private Double successRateLast24h;
    private Double errorRateLast24h;
    private Double avgResponseTimeMs;
    private List<TrendDataPoint> requestsTrend;
    private List<TrendDataPoint> successRateTrend;
    private List<TrendDataPoint> errorRateTrend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendDataPoint {
        private String timestamp;
        private Double value;
    }
}
