package com.nexus.nexusbuddy.service;

import com.nexus.nexusbuddy.payload.ClientInsightsResponse;
import com.nexus.nexusbuddy.payload.ToolInsightsData;
import com.nexus.nexusbuddy.payload.LogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClientInsightsService {

    ClientInsightsResponse getClientInsights(Long clientId, String range, String start, String end);

    Page<ToolInsightsData> getClientToolInsights(Long clientId, String range, String start, String end,
            Pageable pageable);

    Page<LogEntry> getClientLogs(Long clientId, String toolName, String status, String statusCode, String httpMethod,
            String startDate, String endDate, Pageable pageable);
}