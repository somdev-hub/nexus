package com.nexus.nexusbuddy.controller;

import com.nexus.nexusbuddy.annotation.LogActivity;
import com.nexus.nexusbuddy.payload.ClientInsightsResponse;
import com.nexus.nexusbuddy.payload.LogEntry;
import com.nexus.nexusbuddy.payload.ToolInsightsData;
import com.nexus.nexusbuddy.service.ClientInsightsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Client Insights analytics.
 * Provides detailed insights and metrics for a specific client.
 */
@RestController
@RequestMapping("/nexusbuddy/admin/client-insights")
@RequiredArgsConstructor
@Slf4j
public class ClientInsightsController {

    private final ClientInsightsService clientInsightsService;

    /**
     * Get comprehensive client insights including summary, charts, and tools
     * overview.
     * GET /nexusbuddy/admin/client-insights/{clientId}
     *
     * @param clientId Client configuration ID
     * @param range    Time range (24h, 7d, 30d, custom)
     * @param start    Custom range start (ISO8601)
     * @param end      Custom range end (ISO8601)
     * @return ClientInsightsResponse with summary, response time data, hourly hits,
     * failure graph, and tools
     */
    @GetMapping("/{clientId}")
    @LogActivity("GET_CLIENT_INSIGHTS")
    public ResponseEntity<ClientInsightsResponse> getClientInsights(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        log.info("Fetching client insights for clientId: {}, range: {}", clientId, range);
        return ResponseEntity.ok(clientInsightsService.getClientInsights(clientId, range, start, end));
    }

    /**
     * Get paginated tool insights for a specific client.
     * GET /nexusbuddy/admin/client-insights/{clientId}/tools
     *
     * @param clientId   Client configuration ID
     * @param range      Time range (24h, 7d, 30d, custom)
     * @param start      Custom range start (ISO8601)
     * @param end        Custom range end (ISO8601)
     * @param pageNo     Page number (0-based)
     * @param pageOffset Page size
     * @return Page of ToolInsightsData
     */
    @GetMapping("/{clientId}/tools")
    @LogActivity("GET_CLIENT_TOOL_INSIGHTS")
    public ResponseEntity<Page<ToolInsightsData>> getClientToolInsights(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false, defaultValue = "0") Integer pageNo,
            @RequestParam(required = false, defaultValue = "10") Integer pageOffset
    ) {
        log.info("Fetching client tool insights for clientId: {}, range: {}, page: {}, size: {}",
                clientId, range, pageNo, pageOffset);
        Pageable pageable = PageRequest.of(pageNo, pageOffset);
        return ResponseEntity.ok(clientInsightsService.getClientToolInsights(clientId, range, start, end, pageable));
    }

    /**
     * Get paginated logs for a specific client with filtering.
     * GET /nexusbuddy/admin/client-insights/{clientId}/logs
     *
     * @param clientId   Client configuration ID
     * @param toolName   Filter by tool name
     * @param status     Filter by status (success/failure)
     * @param statusCode Filter by HTTP status code
     * @param httpMethod Filter by HTTP method
     * @param startDate  Filter by start date (ISO8601)
     * @param endDate    Filter by end date (ISO8601)
     * @param pageNo     Page number (0-based)
     * @param pageOffset Page size
     * @return Page of LogEntry
     */
    @GetMapping("/{clientId}/logs")
    @LogActivity("GET_CLIENT_LOGS")
    public ResponseEntity<Page<LogEntry>> getClientLogs(
            @PathVariable Long clientId,
            @RequestParam(required = false) String toolName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String statusCode,
            @RequestParam(required = false) String httpMethod,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "0") Integer pageNo,
            @RequestParam(required = false, defaultValue = "10") Integer pageOffset
    ) {
        log.info(
                "Fetching client logs for clientId: {}, toolName: {}, status: {}, statusCode: {}, httpMethod: {}, startDate: {}, endDate: {}, page: {}, size: {}",
                clientId, toolName, status, statusCode, httpMethod, startDate, endDate, pageNo, pageOffset);
        Pageable pageable = PageRequest.of(pageNo, pageOffset);
        return ResponseEntity.ok(clientInsightsService.getClientLogs(clientId, toolName, status, statusCode, httpMethod,
                startDate, endDate, pageable));
    }
}