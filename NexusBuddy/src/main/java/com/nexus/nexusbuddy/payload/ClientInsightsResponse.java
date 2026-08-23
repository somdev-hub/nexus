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
public class ClientInsightsResponse {
    private ClientInsightsSummary summary;
    private List<ResponseTimeDataPoint> responseTimeData;
    private List<HourlyHitsDataPoint> hourlyHitsData;
    private List<FailureGraphDataPoint> failureGraphData;
    private List<ToolInsightsData> tools;
}