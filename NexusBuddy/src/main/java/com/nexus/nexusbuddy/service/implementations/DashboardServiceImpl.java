package com.nexus.nexusbuddy.service.implementations;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.nexusbuddy.model.entities.ClientConfig;
import com.nexus.nexusbuddy.model.entities.NexusBuddyLogs;
import com.nexus.nexusbuddy.model.entities.ToolsConfig;
import com.nexus.nexusbuddy.model.entities.ToolsParamConfig;
import com.nexus.nexusbuddy.payload.ClientHealthResponse;
import com.nexus.nexusbuddy.payload.ConfigInsightsResponse;
import com.nexus.nexusbuddy.payload.DashboardSummaryResponse;
import com.nexus.nexusbuddy.payload.PerformanceResponse;
import com.nexus.nexusbuddy.payload.RequestTrendsResponse;
import com.nexus.nexusbuddy.payload.ToolUsageResponse;
import com.nexus.nexusbuddy.repository.ClientConfigRepository;
import com.nexus.nexusbuddy.repository.NexusBuddyLogsRepo;
import com.nexus.nexusbuddy.repository.ToolsConfigRepository;
import com.nexus.nexusbuddy.repository.ToolsParamConfigRepository;
import com.nexus.nexusbuddy.service.interfaces.DashboardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for Dashboard analytics operations.
 * Provides aggregated insights and metrics for the NexusAdmin dashboard.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final ClientConfigRepository clientConfigRepository;
    private final ToolsConfigRepository toolsConfigRepository;
    private final ToolsParamConfigRepository toolsParamConfigRepository;
    private final NexusBuddyLogsRepo nexusBuddyLogsRepo;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    @Override
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary(
            String range, String start, String end, List<Long> clientIds) {

        log.info("Fetching dashboard summary for range: {}, clientIds: {}", range, clientIds);

        Instant rangeStart = calculateRangeStart(range, start);
        Instant rangeEnd = end != null ? Instant.parse(end) : Instant.now();
        Instant previousRangeStart = rangeStart.minusSeconds(rangeEnd.getEpochSecond() - rangeStart.getEpochSecond());

        // Get all clients (filtered if clientIds provided)
        List<ClientConfig> clients = clientIds != null && !clientIds.isEmpty()
                ? clientConfigRepository.findAllById(clientIds)
                : clientConfigRepository.findAll();

        List<Long> clientConfigIds = clients.stream()
                .map(ClientConfig::getClientConfigId)
                .collect(Collectors.toList());

        // Get tools for these clients
        List<ToolsConfig> tools = toolsConfigRepository
                .findByIsActiveTrueAndClientConfigClientConfigIdIn(clientConfigIds);
        List<Long> toolConfigIds = tools.stream()
                .map(ToolsConfig::getToolsConfigId)
                .collect(Collectors.toList());

        // Get logs in current range
        List<NexusBuddyLogs> currentLogs = nexusBuddyLogsRepo.findByClientConfigClientConfigIdInAndCreatedAtBetween(
                clientConfigIds, Timestamp.from(rangeStart), Timestamp.from(rangeEnd));

        // Get logs in previous range for trend comparison
        List<NexusBuddyLogs> previousLogs = nexusBuddyLogsRepo.findByClientConfigClientConfigIdInAndCreatedAtBetween(
                clientConfigIds, Timestamp.from(previousRangeStart), Timestamp.from(rangeStart));

        // Calculate metrics
        long totalClients = clients.size();
        long activeClients = clients.stream().filter(ClientConfig::getIsActive).count();
        long totalTools = tools.size();
        long activeTools = tools.stream().filter(ToolsConfig::getIsActive).count();
        long requestsLast24h = currentLogs.size();

        long successfulRequests = currentLogs.stream()
                .filter(log -> log.getResponseStatus() >= 200 && log.getResponseStatus() < 300)
                .count();
        double successRateLast24h = requestsLast24h > 0 ? (successfulRequests * 100.0 / requestsLast24h) : 0.0;
        double errorRateLast24h = requestsLast24h > 0
                ? ((requestsLast24h - successfulRequests) * 100.0 / requestsLast24h)
                : 0.0;

        // Average response time (only for logs that have responseTimeMs)
        List<Long> responseTimes = currentLogs.stream()
                .filter(log -> log.getResponseTimeMs() != null)
                .map(NexusBuddyLogs::getResponseTimeMs)
                .collect(Collectors.toList());
        double avgResponseTimeMs = responseTimes.isEmpty() ? 0.0
                : responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);

        // Generate trend data points (simplified - hourly buckets for 24h)
        List<DashboardSummaryResponse.TrendDataPoint> requestsTrend = generateHourlyTrend(currentLogs, rangeStart,
                rangeEnd);
        List<DashboardSummaryResponse.TrendDataPoint> successRateTrend = generateSuccessRateTrend(currentLogs,
                rangeStart, rangeEnd);
        List<DashboardSummaryResponse.TrendDataPoint> errorRateTrend = generateErrorRateTrend(currentLogs, rangeStart,
                rangeEnd);

        DashboardSummaryResponse response = DashboardSummaryResponse.builder()
                .totalClients(totalClients)
                .activeClients(activeClients)
                .totalTools(totalTools)
                .activeTools(activeTools)
                .requestsLast24h(requestsLast24h)
                .successRateLast24h(Math.round(successRateLast24h * 100.0) / 100.0)
                .errorRateLast24h(Math.round(errorRateLast24h * 100.0) / 100.0)
                .avgResponseTimeMs(Math.round(avgResponseTimeMs * 100.0) / 100.0)
                .requestsTrend(requestsTrend)
                .successRateTrend(successRateTrend)
                .errorRateTrend(errorRateTrend)
                .build();

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<ClientHealthResponse>> getClientHealth(
            String range, String start, String end, List<Long> clientIds) {

        log.info("Fetching client health for range: {}, clientIds: {}", range, clientIds);

        Instant rangeStart = calculateRangeStart(range, start);
        Instant rangeEnd = end != null ? Instant.parse(end) : Instant.now();

        List<ClientConfig> clients = clientIds != null && !clientIds.isEmpty()
                ? clientConfigRepository.findAllById(clientIds)
                : clientConfigRepository.findAll();

        List<ClientHealthResponse> healthResponses = clients.stream()
                .map(client -> buildClientHealth(client, rangeStart, rangeEnd))
                .collect(Collectors.toList());

        return ResponseEntity.ok(healthResponses);
    }

    @Override
    public ResponseEntity<RequestTrendsResponse> getRequestTrends(
            String range, String start, String end, List<Long> clientIds) {

        log.info("Fetching request trends for range: {}, clientIds: {}", range, clientIds);

        Instant rangeStart = calculateRangeStart(range, start);
        Instant rangeEnd = end != null ? Instant.parse(end) : Instant.now();

        List<Long> clientConfigIds = clientIds != null && !clientIds.isEmpty()
                ? clientIds
                : clientConfigRepository.findAll().stream()
                        .map(ClientConfig::getClientConfigId)
                        .collect(Collectors.toList());

        List<NexusBuddyLogs> logs = nexusBuddyLogsRepo.findByClientConfigClientConfigIdInAndCreatedAtBetween(
                clientConfigIds, Timestamp.from(rangeStart), Timestamp.from(rangeEnd));

        // Group by time buckets based on range
        int bucketHours = determineBucketHours(range);
        Map<String, List<NexusBuddyLogs>> groupedLogs = logs.stream()
                .collect(Collectors.groupingBy(log -> getTimeBucket(log.getCreatedAt().toInstant(), bucketHours)));

        List<RequestTrendsResponse.TimeSeriesDataPoint> totalRequests = new ArrayList<>();
        List<RequestTrendsResponse.TimeSeriesDataPoint> successfulRequests = new ArrayList<>();
        List<RequestTrendsResponse.TimeSeriesDataPoint> clientErrors = new ArrayList<>();
        List<RequestTrendsResponse.TimeSeriesDataPoint> serverErrors = new ArrayList<>();

        groupedLogs.forEach((bucket, bucketLogs) -> {
            long total = bucketLogs.size();
            long success = bucketLogs.stream()
                    .filter(l -> l.getResponseStatus() >= 200 && l.getResponseStatus() < 300)
                    .count();
            long clientErr = bucketLogs.stream()
                    .filter(l -> l.getResponseStatus() >= 400 && l.getResponseStatus() < 500)
                    .count();
            long serverErr = bucketLogs.stream()
                    .filter(l -> l.getResponseStatus() >= 500)
                    .count();

            totalRequests.add(new RequestTrendsResponse.TimeSeriesDataPoint(bucket, total));
            successfulRequests.add(new RequestTrendsResponse.TimeSeriesDataPoint(bucket, success));
            clientErrors.add(new RequestTrendsResponse.TimeSeriesDataPoint(bucket, clientErr));
            serverErrors.add(new RequestTrendsResponse.TimeSeriesDataPoint(bucket, serverErr));
        });

        // Sort by timestamp
        Comparator<RequestTrendsResponse.TimeSeriesDataPoint> byTime = Comparator
                .comparing(RequestTrendsResponse.TimeSeriesDataPoint::getTimestamp);
        totalRequests.sort(byTime);
        successfulRequests.sort(byTime);
        clientErrors.sort(byTime);
        serverErrors.sort(byTime);

        RequestTrendsResponse response = RequestTrendsResponse.builder()
                .totalRequests(totalRequests)
                .successfulRequests(successfulRequests)
                .clientErrors(clientErrors)
                .serverErrors(serverErrors)
                .build();

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ToolUsageResponse> getToolUsage(
            String range, String start, String end, List<Long> clientIds) {

        log.info("Fetching tool usage for range: {}, clientIds: {}", range, clientIds);

        Instant rangeStart = calculateRangeStart(range, start);
        Instant rangeEnd = end != null ? Instant.parse(end) : Instant.now();

        List<Long> clientConfigIds = clientIds != null && !clientIds.isEmpty()
                ? clientIds
                : clientConfigRepository.findAll().stream()
                        .map(ClientConfig::getClientConfigId)
                        .collect(Collectors.toList());

        List<NexusBuddyLogs> logs = nexusBuddyLogsRepo.findByClientConfigClientConfigIdInAndCreatedAtBetween(
                clientConfigIds, Timestamp.from(rangeStart), Timestamp.from(rangeEnd));

        // HTTP Method Distribution
        Map<String, Long> httpMethodDistribution = logs.stream()
                .collect(Collectors.groupingBy(
                        NexusBuddyLogs::getHttpMethod,
                        Collectors.counting()));

        // Top 10 Tools by Volume
        Map<String, List<NexusBuddyLogs>> logsByTool = logs.stream()
                .collect(Collectors.groupingBy(NexusBuddyLogs::getToolName));

        List<ToolUsageResponse.ToolUsageDataPoint> topToolsByVolume = logsByTool.entrySet()
                .stream().<ToolUsageResponse.ToolUsageDataPoint>map(entry -> {
                    String toolName = entry.getKey();
                    List<NexusBuddyLogs> toolLogs = entry.getValue();
                    long requestCount = toolLogs.size();
                    long successCount = toolLogs.stream()
                            .filter(l -> l.getResponseStatus() >= 200 && l.getResponseStatus() < 300)
                            .count();
                    double successRate = requestCount > 0 ? (successCount * 100.0 / requestCount) : 0.0;
                    return ToolUsageResponse.ToolUsageDataPoint.builder()
                            .toolName(toolName)
                            .requestCount(requestCount)
                            .successRate(Math.round(successRate * 100.0) / 100.0)
                            .build();
                })
                .sorted(Comparator.comparing(ToolUsageResponse.ToolUsageDataPoint::getRequestCount).reversed())
                .limit(10)
                .collect(Collectors.toList());

        ToolUsageResponse response = ToolUsageResponse.builder()
                .httpMethodDistribution(httpMethodDistribution)
                .topToolsByVolume(topToolsByVolume)
                .build();

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<PerformanceResponse> getPerformance(
            String range, String start, String end, List<Long> clientIds) {

        log.info("Fetching performance metrics for range: {}, clientIds: {}", range, clientIds);

        Instant rangeStart = calculateRangeStart(range, start);
        Instant rangeEnd = end != null ? Instant.parse(end) : Instant.now();

        List<Long> clientConfigIds = clientIds != null && !clientIds.isEmpty()
                ? clientIds
                : clientConfigRepository.findAll().stream()
                        .map(ClientConfig::getClientConfigId)
                        .collect(Collectors.toList());

        List<NexusBuddyLogs> logs = nexusBuddyLogsRepo.findByClientConfigClientConfigIdInAndCreatedAtBetween(
                clientConfigIds, Timestamp.from(rangeStart), Timestamp.from(rangeEnd));

        // Latency Percentiles (only for logs with responseTimeMs)
        List<Long> responseTimes = logs.stream()
                .filter(log -> log.getResponseTimeMs() != null)
                .map(NexusBuddyLogs::getResponseTimeMs)
                .sorted()
                .collect(Collectors.toList());

        Map<String, Double> latencyPercentiles = new LinkedHashMap<>();
        if (!responseTimes.isEmpty()) {
            latencyPercentiles.put("P50", percentile(responseTimes, 0.50));
            latencyPercentiles.put("P90", percentile(responseTimes, 0.90));
            latencyPercentiles.put("P95", percentile(responseTimes, 0.95));
            latencyPercentiles.put("P99", percentile(responseTimes, 0.99));
        } else {
            latencyPercentiles.put("P50", 0.0);
            latencyPercentiles.put("P90", 0.0);
            latencyPercentiles.put("P95", 0.0);
            latencyPercentiles.put("P99", 0.0);
        }

        // Error Rate Trend (hourly buckets)
        int bucketHours = determineBucketHours(range);
        Map<String, List<NexusBuddyLogs>> groupedLogs = logs.stream()
                .collect(Collectors.groupingBy(log -> getTimeBucket(log.getCreatedAt().toInstant(), bucketHours)));

        List<PerformanceResponse.TimeSeriesDataPoint> errorRateTrend = groupedLogs.entrySet().stream()
                .map(entry -> {
                    String bucket = entry.getKey();
                    List<NexusBuddyLogs> bucketLogs = entry.getValue();
                    long total = bucketLogs.size();
                    long errors = bucketLogs.stream()
                            .filter(l -> l.getResponseStatus() >= 400)
                            .count();
                    double errorRate = total > 0 ? (errors * 100.0 / total) : 0.0;
                    return new PerformanceResponse.TimeSeriesDataPoint(bucket, Math.round(errorRate * 100.0) / 100.0);
                })
                .sorted(Comparator.comparing(PerformanceResponse.TimeSeriesDataPoint::getTimestamp))
                .collect(Collectors.toList());

        // Availability
        long totalRequests = logs.size();
        long successfulRequests = logs.stream()
                .filter(l -> l.getResponseStatus() >= 200 && l.getResponseStatus() < 300)
                .count();
        double availabilityPercentage = totalRequests > 0 ? (successfulRequests * 100.0 / totalRequests) : 100.0;

        // Tool Success Rates
        Map<String, List<NexusBuddyLogs>> logsByTool = logs.stream()
                .collect(Collectors.groupingBy(NexusBuddyLogs::getToolName));

        List<PerformanceResponse.ToolPerformanceDataPoint> toolSuccessRates = logsByTool.entrySet()
                .stream().<PerformanceResponse.ToolPerformanceDataPoint>map(entry -> {
                    String toolName = entry.getKey();
                    List<NexusBuddyLogs> toolLogs = entry.getValue();
                    long requestCount = toolLogs.size();
                    long successCount = toolLogs.stream()
                            .filter(l -> l.getResponseStatus() >= 200 && l.getResponseStatus() < 300)
                            .count();
                    double successRate = requestCount > 0 ? (successCount * 100.0 / requestCount) : 0.0;

                    List<Long> toolResponseTimes = toolLogs.stream()
                            .filter(l -> l.getResponseTimeMs() != null)
                            .map(NexusBuddyLogs::getResponseTimeMs)
                            .collect(Collectors.toList());
                    double avgLatency = toolResponseTimes.isEmpty() ? 0.0
                            : toolResponseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);

                    return PerformanceResponse.ToolPerformanceDataPoint.builder()
                            .toolName(toolName)
                            .requestCount(requestCount)
                            .successRate(Math.round(successRate * 100.0) / 100.0)
                            .avgLatencyMs(Math.round(avgLatency * 100.0) / 100.0)
                            .build();
                })
                .sorted(Comparator.comparing(PerformanceResponse.ToolPerformanceDataPoint::getRequestCount).reversed())
                .collect(Collectors.toList());

        PerformanceResponse response = PerformanceResponse.builder()
                .latencyPercentiles(latencyPercentiles)
                .errorRateTrend(errorRateTrend)
                .availabilityPercentage(Math.round(availabilityPercentage * 100.0) / 100.0)
                .toolSuccessRates(toolSuccessRates)
                .build();

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ConfigInsightsResponse> getConfigInsights() {

        log.info("Fetching configuration insights");

        List<ClientConfig> clients = clientConfigRepository.findAll();
        List<ToolsConfig> tools = toolsConfigRepository.findAll();
        List<ToolsParamConfig> params = toolsParamConfigRepository.findAll();

        // Tools per Client
        Map<String, Long> toolsPerClient = tools.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getClientConfig().getClientName(),
                        Collectors.counting()));

        // Params per Tool
        Map<String, Long> paramsPerTool = params.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getToolsConfig().getToolName(),
                        Collectors.counting()));

        // Required vs Optional Params
        Map<String, Long> requiredVsOptionalParams = params.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getIsRequired() ? "Required" : "Optional",
                        Collectors.counting()));

        // Data Type Distribution
        Map<String, Long> dataTypeDistribution = params.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getDataType().name(),
                        Collectors.counting()));

        // Param Type Distribution
        Map<String, Long> paramTypeDistribution = params.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getParamType().name(),
                        Collectors.counting()));

        // Clients Without Tools
        Set<Long> clientIdsWithTools = tools.stream()
                .map(t -> t.getClientConfig().getClientConfigId())
                .collect(Collectors.toSet());
        List<String> clientsWithoutTools = clients.stream()
                .filter(c -> !clientIdsWithTools.contains(c.getClientConfigId()))
                .map(ClientConfig::getClientName)
                .collect(Collectors.toList());

        // Tools Without Params
        Set<Long> toolIdsWithParams = params.stream()
                .map(p -> p.getToolsConfig().getToolsConfigId())
                .collect(Collectors.toSet());
        List<String> toolsWithoutParams = tools.stream()
                .filter(t -> !toolIdsWithParams.contains(t.getToolsConfigId()))
                .map(ToolsConfig::getToolName)
                .collect(Collectors.toList());

        ConfigInsightsResponse response = ConfigInsightsResponse.builder()
                .toolsPerClient(toolsPerClient)
                .paramsPerTool(paramsPerTool)
                .requiredVsOptionalParams(requiredVsOptionalParams)
                .dataTypeDistribution(dataTypeDistribution)
                .paramTypeDistribution(paramTypeDistribution)
                .clientsWithoutTools(clientsWithoutTools)
                .toolsWithoutParams(toolsWithoutParams)
                .build();

        return ResponseEntity.ok(response);
    }

    // Helper methods
    private Instant calculateRangeStart(String range, String start) {
        if ("custom".equals(range) && start != null) {
            return Instant.parse(start);
        }
        Instant now = Instant.now();
        return switch (range) {
            case "7d" -> now.minusSeconds(7 * 24 * 60 * 60);
            case "30d" -> now.minusSeconds(30 * 24 * 60 * 60);
            case "24h" -> now.minusSeconds(24 * 60 * 60);
            default -> now.minusSeconds(24 * 60 * 60);
        };
    }

    private int determineBucketHours(String range) {
        return switch (range) {
            case "7d" -> 6; // 6-hour buckets for 7 days = 28 points
            case "30d" -> 24; // Daily buckets for 30 days = 30 points
            case "24h" -> 1; // Hourly buckets for 24 hours = 24 points
            default -> 1;
        };
    }

    private String getTimeBucket(Instant instant, int bucketHours) {
        LocalDateTime ldt = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
        int hour = ldt.getHour();
        int bucketHour = (hour / bucketHours) * bucketHours;
        return ldt.withHour(bucketHour).withMinute(0).withSecond(0).withNano(0)
                .atZone(ZoneId.systemDefault()).toInstant().toString();
    }

    private List<DashboardSummaryResponse.TrendDataPoint> generateHourlyTrend(
            List<NexusBuddyLogs> logs, Instant start, Instant end) {
        int bucketHours = 1;
        Map<String, Long> grouped = logs.stream()
                .collect(Collectors.groupingBy(
                        log -> getTimeBucket(log.getCreatedAt().toInstant(), bucketHours),
                        Collectors.counting()));
        return grouped.entrySet().stream()
                .map(e -> new DashboardSummaryResponse.TrendDataPoint(e.getKey(), e.getValue().doubleValue()))
                .sorted(Comparator.comparing(DashboardSummaryResponse.TrendDataPoint::getTimestamp))
                .collect(Collectors.toList());
    }

    private List<DashboardSummaryResponse.TrendDataPoint> generateSuccessRateTrend(
            List<NexusBuddyLogs> logs, Instant start, Instant end) {
        int bucketHours = 1;
        Map<String, List<NexusBuddyLogs>> grouped = logs.stream()
                .collect(Collectors.groupingBy(
                        log -> getTimeBucket(log.getCreatedAt().toInstant(), bucketHours)));
        return grouped.entrySet().stream()
                .map(e -> {
                    long total = e.getValue().size();
                    long success = e.getValue().stream()
                            .filter(l -> l.getResponseStatus() >= 200 && l.getResponseStatus() < 300)
                            .count();
                    double rate = total > 0 ? (success * 100.0 / total) : 0.0;
                    return new DashboardSummaryResponse.TrendDataPoint(e.getKey(), Math.round(rate * 100.0) / 100.0);
                })
                .sorted(Comparator.comparing(DashboardSummaryResponse.TrendDataPoint::getTimestamp))
                .collect(Collectors.toList());
    }

    private List<DashboardSummaryResponse.TrendDataPoint> generateErrorRateTrend(
            List<NexusBuddyLogs> logs, Instant start, Instant end) {
        int bucketHours = 1;
        Map<String, List<NexusBuddyLogs>> grouped = logs.stream()
                .collect(Collectors.groupingBy(
                        log -> getTimeBucket(log.getCreatedAt().toInstant(), bucketHours)));
        return grouped.entrySet().stream()
                .map(e -> {
                    long total = e.getValue().size();
                    long errors = e.getValue().stream()
                            .filter(l -> l.getResponseStatus() >= 400)
                            .count();
                    double rate = total > 0 ? (errors * 100.0 / total) : 0.0;
                    return new DashboardSummaryResponse.TrendDataPoint(e.getKey(), Math.round(rate * 100.0) / 100.0);
                })
                .sorted(Comparator.comparing(DashboardSummaryResponse.TrendDataPoint::getTimestamp))
                .collect(Collectors.toList());
    }

    private ClientHealthResponse buildClientHealth(ClientConfig client, Instant rangeStart, Instant rangeEnd) {
        List<ToolsConfig> clientTools = toolsConfigRepository
                .findByClientConfigClientConfigId(client.getClientConfigId(), Pageable.unpaged()).getContent();
        long toolCount = clientTools.size();
        long activeToolCount = clientTools.stream().filter(ToolsConfig::getIsActive).count();

        List<NexusBuddyLogs> logs = nexusBuddyLogsRepo.findByClientConfigClientConfigIdAndCreatedAtBetween(
                client.getClientConfigId(), Timestamp.from(rangeStart), Timestamp.from(rangeEnd));

        long requestsLast24h = logs.size();
        long errorsLast24h = logs.stream()
                .filter(l -> l.getResponseStatus() >= 400)
                .count();
        double errorRateLast24h = requestsLast24h > 0 ? (errorsLast24h * 100.0 / requestsLast24h) : 0.0;

        Optional<NexusBuddyLogs> lastLog = logs.stream()
                .max(Comparator.comparing(NexusBuddyLogs::getCreatedAt));
        String lastRequestTime = lastLog.map(l -> l.getCreatedAt().toInstant().toString()).orElse(null);

        // Health check status - simplified (would need async HTTP call in real
        // implementation)
        Boolean healthCheckStatus = client.getIsActive(); // Placeholder

        return ClientHealthResponse.builder()
                .clientConfigId(client.getClientConfigId())
                .clientName(client.getClientName())
                .isActive(client.getIsActive())
                .toolCount(toolCount)
                .activeToolCount(activeToolCount)
                .requestsLast24h(requestsLast24h)
                .errorsLast24h(errorsLast24h)
                .errorRateLast24h(Math.round(errorRateLast24h * 100.0) / 100.0)
                .lastRequestTime(lastRequestTime)
                .healthCheckStatus(healthCheckStatus)
                .connectionUrl(client.getConnectionUrl())
                .healthCheckPath(client.getHealthCheckPath())
                .build();
    }

    private double percentile(List<Long> sortedValues, double percentile) {
        if (sortedValues.isEmpty())
            return 0.0;
        int index = (int) Math.ceil(percentile * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }
}