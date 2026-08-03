package com.nexus.nexusbuddy.service.interfaces;

import com.nexus.nexusbuddy.payload.ClientHealthResponse;
import com.nexus.nexusbuddy.payload.ConfigInsightsResponse;
import com.nexus.nexusbuddy.payload.DashboardSummaryResponse;
import com.nexus.nexusbuddy.payload.PerformanceResponse;
import com.nexus.nexusbuddy.payload.RequestTrendsResponse;
import com.nexus.nexusbuddy.payload.ToolUsageResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Service interface for Dashboard analytics operations.
 * Provides aggregated insights and metrics for the NexusAdmin dashboard.
 */
public interface DashboardService {

    /**
     * Get executive summary metrics for the dashboard.
     * 
     * @param range     Time range (24h, 7d, 30d, custom)
     * @param start     Custom range start (ISO8601)
     * @param end       Custom range end (ISO8601)
     * @param clientIds Optional filter for specific clients
     * @return DashboardSummaryResponse with key metrics
     */
    ResponseEntity<DashboardSummaryResponse> getDashboardSummary(
            String range, String start, String end, List<Long> clientIds);

    /**
     * Get client health matrix data.
     * 
     * @param range     Time range (24h, 7d, 30d, custom)
     * @param start     Custom range start (ISO8601)
     * @param end       Custom range end (ISO8601)
     * @param clientIds Optional filter for specific clients
     * @return List of ClientHealthResponse
     */
    ResponseEntity<List<ClientHealthResponse>> getClientHealth(
            String range, String start, String end, List<Long> clientIds);

    /**
     * Get request volume trends over time.
     * 
     * @param range     Time range (24h, 7d, 30d, custom)
     * @param start     Custom range start (ISO8601)
     * @param end       Custom range end (ISO8601)
     * @param clientIds Optional filter for specific clients
     * @return RequestTrendsResponse with time series data
     */
    ResponseEntity<RequestTrendsResponse> getRequestTrends(
            String range, String start, String end, List<Long> clientIds);

    /**
     * Get tool usage analytics.
     * 
     * @param range     Time range (24h, 7d, 30d, custom)
     * @param start     Custom range start (ISO8601)
     * @param end       Custom range end (ISO8601)
     * @param clientIds Optional filter for specific clients
     * @return ToolUsageResponse with method distribution and top tools
     */
    ResponseEntity<ToolUsageResponse> getToolUsage(
            String range, String start, String end, List<Long> clientIds);

    /**
     * Get performance and reliability metrics.
     * 
     * @param range     Time range (24h, 7d, 30d, custom)
     * @param start     Custom range start (ISO8601)
     * @param end       Custom range end (ISO8601)
     * @param clientIds Optional filter for specific clients
     * @return PerformanceResponse with latency percentiles and error trends
     */
    ResponseEntity<PerformanceResponse> getPerformance(
            String range, String start, String end, List<Long> clientIds);

    /**
     * Get configuration insights (static analysis).
     * 
     * @return ConfigInsightsResponse with configuration analytics
     */
    ResponseEntity<ConfigInsightsResponse> getConfigInsights();
}