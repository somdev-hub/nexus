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
public class RequestTrendsResponse {
    private List<TimeSeriesDataPoint> totalRequests;
    private List<TimeSeriesDataPoint> successfulRequests;
    private List<TimeSeriesDataPoint> clientErrors;
    private List<TimeSeriesDataPoint> serverErrors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesDataPoint {
        private String timestamp;
        private Long value;
    }
}
