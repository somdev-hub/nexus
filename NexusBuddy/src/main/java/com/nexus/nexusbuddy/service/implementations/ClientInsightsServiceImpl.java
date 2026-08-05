package com.nexus.nexusbuddy.service.implementations;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.nexusbuddy.model.entities.ClientConfig;
import com.nexus.nexusbuddy.model.entities.NexusBuddyLogs;
import com.nexus.nexusbuddy.model.entities.ToolsConfig;
import com.nexus.nexusbuddy.payload.ClientInsightsResponse;
import com.nexus.nexusbuddy.payload.ClientInsightsSummary;
import com.nexus.nexusbuddy.payload.FailureGraphDataPoint;
import com.nexus.nexusbuddy.payload.HourlyHitsDataPoint;
import com.nexus.nexusbuddy.payload.LogEntry;
import com.nexus.nexusbuddy.payload.ResponseTimeDataPoint;
import com.nexus.nexusbuddy.payload.ToolInsightsData;
import com.nexus.nexusbuddy.repository.ClientConfigRepository;
import com.nexus.nexusbuddy.repository.NexusBuddyLogsRepo;
import com.nexus.nexusbuddy.repository.ToolsConfigRepository;
import com.nexus.nexusbuddy.service.ClientInsightsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for Client Insights operations.
 * Provides detailed analytics and insights for a specific client.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ClientInsightsServiceImpl implements ClientInsightsService {

    private final ClientConfigRepository clientConfigRepository;
    private final ToolsConfigRepository toolsConfigRepository;
    private final NexusBuddyLogsRepo nexusBuddyLogsRepo;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    @Override
    public ClientInsightsResponse getClientInsights(Long clientId, String range, String start, String end) {
        log.info("Fetching client insights for clientId: {}, range: {}", clientId, range);

        Instant rangeStart = calculateRangeStart(range, start);
        Instant rangeEnd = end != null ? Instant.parse(end) : Instant.now();

        // Verify client exists
        ClientConfig client = clientConfigRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        // Get tools for this client
        List<ToolsConfig> tools = toolsConfigRepository
                .findByClientConfigClientConfigId(clientId, org.springframework.data.domain.Pageable.unpaged())
                .getContent();
        List<Long> toolConfigIds = tools.stream()
                .map(ToolsConfig::getToolsConfigId)
                .collect(Collectors.toList());

        // Get logs for this client in the range
        List<NexusBuddyLogs> logs = nexusBuddyLogsRepo.findByClientConfigClientConfigIdAndCreatedAtBetween(
                clientId, Timestamp.from(rangeStart), Timestamp.from(rangeEnd));

        // Calculate summary metrics
        long totalHits = logs.size();
        long successfulHits = logs.stream()
                .filter(log -> log.getResponseStatus() >= 200 && log.getResponseStatus() < 300)
                .count();
        long failureHits = totalHits - successfulHits;
        double successPercentage = totalHits > 0 ? (successfulHits * 100.0 / totalHits) : 0.0;
        double failurePercentage = totalHits > 0 ? (failureHits * 100.0 / totalHits) : 0.0;

        List<Long> responseTimes = logs.stream()
                .filter(log -> log.getResponseTimeMs() != null)
                .map(NexusBuddyLogs::getResponseTimeMs)
                .collect(Collectors.toList());
        double avgResponseTimeMs = responseTimes.isEmpty() ? 0.0
                : responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);

        ClientInsightsSummary summary = ClientInsightsSummary.builder()
                .totalHits(totalHits)
                .successPercentage(Math.round(successPercentage * 100.0) / 100.0)
                .failurePercentage(Math.round(failurePercentage * 100.0) / 100.0)
                .averageResponseTime(Math.round(avgResponseTimeMs * 100.0) / 100.0)
                .build();

        // Generate response time data (hourly buckets)
        List<ResponseTimeDataPoint> responseTimeData = generateResponseTimeData(logs, rangeStart, rangeEnd);

        // Generate hourly hits data
        List<HourlyHitsDataPoint> hourlyHitsData = generateHourlyHitsData(logs, rangeStart, rangeEnd);

        // Generate failure graph data
        List<FailureGraphDataPoint> failureGraphData = generateFailureGraphData(logs, rangeStart, rangeEnd);

        // Generate tools insights
        List<ToolInsightsData> toolsInsights = generateToolsInsights(logs);

        return ClientInsightsResponse.builder()
                .summary(summary)
                .responseTimeData(responseTimeData)
                .hourlyHitsData(hourlyHitsData)
                .failureGraphData(failureGraphData)
                .tools(toolsInsights)
                .build();
    }

    @Override
    public Page<ToolInsightsData> getClientToolInsights(Long clientId, String range, String start, String end,
            Pageable pageable) {
        log.info("Fetching client tool insights for clientId: {}, range: {}, page: {}, size: {}",
                clientId, range, pageable.getPageNumber(), pageable.getPageSize());

        Instant rangeStart = calculateRangeStart(range, start);
        Instant rangeEnd = end != null ? Instant.parse(end) : Instant.now();

        // Verify client exists
        clientConfigRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        // Get logs for this client in the range
        List<NexusBuddyLogs> logs = nexusBuddyLogsRepo.findByClientConfigClientConfigIdAndCreatedAtBetween(
                clientId, Timestamp.from(rangeStart), Timestamp.from(rangeEnd));

        // Group by tool name
        Map<String, List<NexusBuddyLogs>> logsByTool = logs.stream()
                .collect(Collectors.groupingBy(NexusBuddyLogs::getToolName));

        List<ToolInsightsData> allTools = logsByTool.entrySet().stream()
                .map(entry -> {
                    String toolName = entry.getKey();
                    List<NexusBuddyLogs> toolLogs = entry.getValue();
                    long totalHits = toolLogs.size();
                    long successHits = toolLogs.stream()
                            .filter(l -> l.getResponseStatus() >= 200 && l.getResponseStatus() < 300)
                            .count();
                    long failureHits = totalHits - successHits;

                    // Find most common failure code
                    String mostFailureCode = toolLogs.stream()
                            .filter(l -> l.getResponseStatus() >= 400)
                            .collect(Collectors.groupingBy(
                                    NexusBuddyLogs::getResponseStatus,
                                    Collectors.counting()))
                            .entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(e -> e.getKey().toString())
                            .orElse("N/A");

                    double successRatio = totalHits > 0 ? (successHits * 100.0 / totalHits) : 0.0;

                    return ToolInsightsData.builder()
                            .toolName(toolName)
                            .totalHits(totalHits)
                            .successHits(successHits)
                            .failureHits(failureHits)
                            .mostFailureCode(mostFailureCode)
                            .successRatio(Math.round(successRatio * 100.0) / 100.0)
                            .build();
                })
                .sorted(Comparator.comparing(ToolInsightsData::getTotalHits).reversed())
                .collect(Collectors.toList());

        // Apply pagination
        int startIdx = (int) pageable.getOffset();
        int endIdx = Math.min(startIdx + pageable.getPageSize(), allTools.size());
        List<ToolInsightsData> pageContent = startIdx < allTools.size()
                ? allTools.subList(startIdx, endIdx)
                : new ArrayList<>();

        return new PageImpl<>(pageContent, pageable, allTools.size());
    }

    @Override
    public Page<LogEntry> getClientLogs(Long clientId, String toolName, String status, String statusCode,
            String httpMethod, String startDate, String endDate, Pageable pageable) {
        log.info(
                "Fetching client logs for clientId: {}, toolName: {}, status: {}, statusCode: {}, httpMethod: {}, startDate: {}, endDate: {}, page: {}, size: {}",
                clientId, toolName, status, statusCode, httpMethod, startDate, endDate, pageable.getPageNumber(),
                pageable.getPageSize());

        // Verify client exists
        clientConfigRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        // Build query with filters
        Instant start = startDate != null ? Instant.parse(startDate) : null;
        Instant end = endDate != null ? Instant.parse(endDate) : Instant.now();

        List<NexusBuddyLogs> logs;
        if (start != null) {
            logs = nexusBuddyLogsRepo.findByClientConfigClientConfigIdAndCreatedAtBetween(
                    clientId, Timestamp.from(start), Timestamp.from(end));
        } else {
            // Default to last 24 hours if no start date
            Instant defaultStart = Instant.now().minusSeconds(24 * 60 * 60);
            logs = nexusBuddyLogsRepo.findByClientConfigClientConfigIdAndCreatedAtBetween(
                    clientId, Timestamp.from(defaultStart), Timestamp.from(end));
        }

        // Apply filters
        if (toolName != null && !toolName.isEmpty()) {
            logs = logs.stream().filter(l -> toolName.equals(l.getToolName())).collect(Collectors.toList());
        }
        if (status != null && !status.isEmpty()) {
            if ("success".equalsIgnoreCase(status)) {
                logs = logs.stream().filter(l -> l.getResponseStatus() >= 200 && l.getResponseStatus() < 300)
                        .collect(Collectors.toList());
            } else if ("failure".equalsIgnoreCase(status)) {
                logs = logs.stream().filter(l -> l.getResponseStatus() >= 400).collect(Collectors.toList());
            }
        }
        if (statusCode != null && !statusCode.isEmpty()) {
            try {
                int code = Integer.parseInt(statusCode);
                logs = logs.stream().filter(l -> code == l.getResponseStatus()).collect(Collectors.toList());
            } catch (NumberFormatException e) {
                log.warn("Invalid status code filter: {}", statusCode);
            }
        }
        if (httpMethod != null && !httpMethod.isEmpty()) {
            logs = logs.stream().filter(l -> httpMethod.equalsIgnoreCase(l.getHttpMethod()))
                    .collect(Collectors.toList());
        }

        // Sort by createdAt descending (most recent first)
        logs.sort(Comparator.comparing(NexusBuddyLogs::getCreatedAt).reversed());

        // Convert to LogEntry DTOs
        List<LogEntry> allEntries = logs.stream()
                .map(this::convertToLogEntry)
                .collect(Collectors.toList());

        // Apply pagination
        int startIdx = (int) pageable.getOffset();
        int endIdx = Math.min(startIdx + pageable.getPageSize(), allEntries.size());
        List<LogEntry> pageContent = startIdx < allEntries.size()
                ? allEntries.subList(startIdx, endIdx)
                : new ArrayList<>();

        return new PageImpl<>(pageContent, pageable, allEntries.size());
    }

    private LogEntry convertToLogEntry(NexusBuddyLogs log) {
        return LogEntry.builder()
                .toolName(log.getToolName())
                .request(log.getRequest() != null ? log.getRequest().toString() : null)
                .response(log.getResponse() != null ? log.getResponse().toString() : null)
                .statusCode(log.getResponseStatus())
                .httpMethod(log.getHttpMethod())
                .createdAt(log.getCreatedAt() != null ? log.getCreatedAt().toInstant().toString() : null)
                .build();
    }

    private List<ResponseTimeDataPoint> generateResponseTimeData(List<NexusBuddyLogs> logs, Instant start,
            Instant end) {
        int bucketHours = determineBucketHours(start, end);
        Map<String, List<NexusBuddyLogs>> grouped = logs.stream()
                .filter(log -> log.getResponseTimeMs() != null)
                .collect(Collectors.groupingBy(log -> getTimeBucket(log.getCreatedAt().toInstant(), bucketHours)));

        return grouped.entrySet().stream()
                .map(entry -> {
                    String bucket = entry.getKey();
                    List<NexusBuddyLogs> bucketLogs = entry.getValue();
                    List<Long> times = bucketLogs.stream()
                            .map(NexusBuddyLogs::getResponseTimeMs)
                            .collect(Collectors.toList());
                    double avg = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
                    long min = times.stream().mapToLong(Long::longValue).min().orElse(0L);
                    long max = times.stream().mapToLong(Long::longValue).max().orElse(0L);
                    return ResponseTimeDataPoint.builder()
                            .timestamp(bucket)
                            .avgResponseTime(Math.round(avg * 100.0) / 100.0)
                            .minResponseTime((double) min)
                            .maxResponseTime((double) max)
                            .build();
                })
                .sorted(Comparator.comparing(ResponseTimeDataPoint::getTimestamp))
                .collect(Collectors.toList());
    }

    private List<HourlyHitsDataPoint> generateHourlyHitsData(List<NexusBuddyLogs> logs, Instant start, Instant end) {
        int bucketHours = determineBucketHours(start, end);
        Map<String, List<NexusBuddyLogs>> grouped = logs.stream()
                .collect(Collectors.groupingBy(log -> getTimeBucket(log.getCreatedAt().toInstant(), bucketHours)));

        return grouped.entrySet().stream()
                .map(entry -> {
                    String bucket = entry.getKey();
                    List<NexusBuddyLogs> bucketLogs = entry.getValue();
                    long total = bucketLogs.size();
                    long success = bucketLogs.stream()
                            .filter(l -> l.getResponseStatus() >= 200 && l.getResponseStatus() < 300)
                            .count();
                    long failure = total - success;
                    return HourlyHitsDataPoint.builder()
                            .hour(bucket)
                            .totalHits(total)
                            .successHits(success)
                            .failureHits(failure)
                            .build();
                })
                .sorted(Comparator.comparing(HourlyHitsDataPoint::getHour))
                .collect(Collectors.toList());
    }

    private List<FailureGraphDataPoint> generateFailureGraphData(List<NexusBuddyLogs> logs, Instant start,
            Instant end) {
        int bucketHours = determineBucketHours(start, end);
        Map<String, List<NexusBuddyLogs>> grouped = logs.stream()
                .collect(Collectors.groupingBy(log -> getTimeBucket(log.getCreatedAt().toInstant(), bucketHours)));

        return grouped.entrySet().stream()
                .map(entry -> {
                    String bucket = entry.getKey();
                    List<NexusBuddyLogs> bucketLogs = entry.getValue();
                    long total = bucketLogs.size();
                    long failures = bucketLogs.stream()
                            .filter(l -> l.getResponseStatus() >= 400)
                            .count();
                    double failureRate = total > 0 ? (failures * 100.0 / total) : 0.0;
                    return FailureGraphDataPoint.builder()
                            .timestamp(bucket)
                            .failureCount(failures)
                            .failureRate(Math.round(failureRate * 100.0) / 100.0)
                            .build();
                })
                .sorted(Comparator.comparing(FailureGraphDataPoint::getTimestamp))
                .collect(Collectors.toList());
    }

    private List<ToolInsightsData> generateToolsInsights(List<NexusBuddyLogs> logs) {
        Map<String, List<NexusBuddyLogs>> logsByTool = logs.stream()
                .collect(Collectors.groupingBy(NexusBuddyLogs::getToolName));

        return logsByTool.entrySet().stream()
                .map(entry -> {
                    String toolName = entry.getKey();
                    List<NexusBuddyLogs> toolLogs = entry.getValue();
                    long totalHits = toolLogs.size();
                    long successHits = toolLogs.stream()
                            .filter(l -> l.getResponseStatus() >= 200 && l.getResponseStatus() < 300)
                            .count();
                    long failureHits = totalHits - successHits;

                    String mostFailureCode = toolLogs.stream()
                            .filter(l -> l.getResponseStatus() >= 400)
                            .collect(Collectors.groupingBy(
                                    NexusBuddyLogs::getResponseStatus,
                                    Collectors.counting()))
                            .entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(e -> e.getKey().toString())
                            .orElse("N/A");

                    double successRatio = totalHits > 0 ? (successHits * 100.0 / totalHits) : 0.0;

                    return ToolInsightsData.builder()
                            .toolName(toolName)
                            .totalHits(totalHits)
                            .successHits(successHits)
                            .failureHits(failureHits)
                            .mostFailureCode(mostFailureCode)
                            .successRatio(Math.round(successRatio * 100.0) / 100.0)
                            .build();
                })
                .sorted(Comparator.comparing(ToolInsightsData::getTotalHits).reversed())
                .collect(Collectors.toList());
    }

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

    private int determineBucketHours(Instant start, Instant end) {
        long hoursBetween = java.time.Duration.between(start, end).toHours();
        if (hoursBetween <= 24)
            return 1; // Hourly buckets for 24h
        if (hoursBetween <= 168)
            return 6; // 6-hour buckets for 7d
        return 24; // Daily buckets for 30d+
    }

    private String getTimeBucket(Instant instant, int bucketHours) {
        LocalDateTime ldt = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
        int hour = ldt.getHour();
        int bucketHour = (hour / bucketHours) * bucketHours;
        return ldt.withHour(bucketHour).withMinute(0).withSecond(0).withNano(0)
                .atZone(ZoneId.systemDefault()).toInstant().toString();
    }
}