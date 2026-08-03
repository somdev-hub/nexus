package com.nexus.nexusbuddy.controller;

import com.nexus.nexusbuddy.annotation.LogActivity;
import com.nexus.nexusbuddy.payload.ClientHealthResponse;
import com.nexus.nexusbuddy.payload.ConfigInsightsResponse;
import com.nexus.nexusbuddy.payload.DashboardSummaryResponse;
import com.nexus.nexusbuddy.payload.PerformanceResponse;
import com.nexus.nexusbuddy.payload.RequestTrendsResponse;
import com.nexus.nexusbuddy.payload.ToolUsageResponse;
import com.nexus.nexusbuddy.service.interfaces.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Dashboard analytics.
 * Provides aggregated insights and metrics for the NexusAdmin dashboard.
 */
@RestController
@RequestMapping("/nexusbuddy/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Get executive summary metrics for the dashboard.
     * GET /nexusbuddy/admin/dashboard/summary
     * 
     * @param range     Time range (24h, 7d, 30d, custom)
     * @param start     Custom range start (ISO8601)
     * @param end       Custom range end (ISO8601)
     * @param clientIds Optional filter for specific clients
     * @return DashboardSummaryResponse with key metrics
     */
    @GetMapping("/summary")
    @LogActivity("GET_DASHBOARD_SUMMARY")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary(
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) List<Long> clientIds) {
        log.info("Fetching dashboard summary for range: {}, clientIds: {}", range, clientIds);
        return dashboardService.getDashboardSummary(range, start, end, clientIds);
    }

    /**
     * Get client health matrix data.
     * GET /nexusbuddy/admin/dashboard/client-health
     * 
     * @param range     Time range (24h, 7d, 30d, custom)
     * @param start     Custom range start (ISO8601)
     * @param end       Custom range end (ISO8601)
     * @param clientIds Optional filter for specific clients
     * @return List of ClientHealthResponse
     */
    @GetMapping("/client-health")
    @LogActivity("GET_CLIENT_HEALTH")
    public ResponseEntity<List<ClientHealthResponse>> getClientHealth(
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) List<Long> clientIds) {
        log.info("Fetching client health for range: {}, clientIds: {}", range, clientIds);
        return dashboardService.getClientHealth(range, start, end, clientIds);
    }

    /**
     * Get request volume trends over time.
     * GET /nexusbuddy/admin/dashboard/requests/trends
     * 
     * @param range     Time range (24h, 7d, 30d, custom)
     * @param start     Custom range start (ISO8601)
     * @param end       Custom range end (ISO8601)
     * @param clientIds Optional filter for specific clients
     * @return RequestTrendsResponse with time series data
     */
    @GetMapping("/requests/trends")
    @LogActivity("GET_REQUEST_TRENDS")
    public ResponseEntity<RequestTrendsResponse> getRequestTrends(
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) List<Long> clientIds) {
        log.info("Fetching request trends for range: {}, clientIds: {}", range, clientIds);
        return dashboardService.getRequestTrends(range, start, end, clientIds);
    }

    /**
     * Get tool usage analytics.
     * GET /nexusbuddy/admin/dashboard/tools/usage
     * 
     * @param range     Time range (24h, 7d, 30d, custom)
     * @param start     Custom range start (ISO8601)
     * @param end       Custom range end (ISO8601)
     * @param clientIds Optional filter for specific clients
     * @return ToolUsageResponse with method distribution and top tools
     */
    @GetMapping("/tools/usage")
    @LogActivity("GET_TOOL_USAGE")
    public ResponseEntity<ToolUsageResponse> getToolUsage(
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) List<Long> clientIds) {
        log.info("Fetching tool usage for range: {}, clientIds: {}", range, clientIds);
        return dashboardService.getToolUsage(range, start, end, clientIds);
    }

    /**
     * Get performance and reliability metrics.
     * GET /nexusbuddy/admin/dashboard/performance
     * 
     * @param range     Time range (24h, 7d, 30d, custom)
     * @param start     Custom range start (ISO8601)
     * @param end       Custom range end (ISO8601)
     * @param clientIds Optional filter for specific clients
     * @return PerformanceResponse with latency percentiles and error trends
     */
    @GetMapping("/performance")
    @LogActivity("GET_PERFORMANCE")
    public ResponseEntity<PerformanceResponse> getPerformance(
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) List<Long> clientIds) {
        log.info("Fetching performance metrics for range: {}, clientIds: {}", range, clientIds);
        return dashboardService.getPerformance(range, start, end, clientIds);
    }

    /**
     * Get configuration insights (static analysis).
     * GET /nexusbuddy/admin/dashboard/config-insights
     * 
     * @return ConfigInsightsResponse with configuration analytics
     */
    @GetMapping("/config-insights")
    @LogActivity("GET_CONFIG_INSIGHTS")
    public ResponseEntity<ConfigInsightsResponse> getConfigInsights() {
        log.info("Fetching configuration insights");
        return dashboardService.getConfigInsights();
    }
}