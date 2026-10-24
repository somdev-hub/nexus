package com.nexus.cms.notification.service.implementations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads outbox analytics directly from core.t_event_outbox using JdbcTemplate.
 * No entity mapping needed — the outbox lives in the core schema but CMS
 * shares the same database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxAnalyticsServiceImpl {

    private final JdbcTemplate jdbcTemplate;

    public Map<String, Object> summary() {
        String sql = """
                SELECT
                    COUNT(*) FILTER (WHERE status = 'PUBLISHED' AND attempts = 1) AS direct_kafka,
                    COUNT(*) FILTER (WHERE status = 'PUBLISHED' AND attempts > 1) AS retried_lookup,
                    COUNT(*) FILTER (WHERE status = 'FAILED') AS gave_up,
                    COUNT(*) FILTER (WHERE status = 'PENDING') AS pending,
                    COUNT(*) AS total
                FROM core.t_event_outbox
                """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        if (rows.isEmpty()) return Map.of("directKafka", 0, "retriedLookup", 0, "gaveUp", 0, "pending", 0, "total", 0);
        Map<String, Object> r = rows.get(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("directKafka", r.get("direct_kafka"));
        result.put("retriedLookup", r.get("retried_lookup"));
        result.put("gaveUp", r.get("gave_up"));
        result.put("pending", r.get("pending"));
        result.put("total", r.get("total"));
        return result;
    }

    public List<Map<String, Object>> byEventType() {
        String sql = """
                SELECT event_type,
                       COUNT(*) FILTER (WHERE status = 'PUBLISHED' AND attempts = 1) AS direct,
                       COUNT(*) FILTER (WHERE status = 'PUBLISHED' AND attempts > 1) AS retried,
                       COUNT(*) FILTER (WHERE status = 'FAILED') AS failed,
                       COUNT(*) AS total,
                       COALESCE(AVG(EXTRACT(EPOCH FROM (published_at - created_at))) FILTER (WHERE status = 'PUBLISHED'), 0) AS avg_lag_sec
                FROM core.t_event_outbox
                GROUP BY event_type
                ORDER BY COUNT(*) DESC
                """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("eventType", r.get("event_type"));
            entry.put("direct", r.get("direct"));
            entry.put("retried", r.get("retried"));
            entry.put("failed", r.get("failed"));
            entry.put("total", r.get("total"));
            entry.put("avgLagSec", r.get("avg_lag_sec"));
            result.add(entry);
        }
        return result;
    }

    public List<Map<String, Object>> trend(int days) {
        Timestamp from = Timestamp.from(Instant.now().minus(days, ChronoUnit.DAYS));
        String sql = """
                SELECT created_at::date AS day,
                       COUNT(*) FILTER (WHERE status = 'PUBLISHED' AND attempts = 1) AS direct,
                       COUNT(*) FILTER (WHERE status = 'PUBLISHED' AND attempts > 1) AS retried,
                       COUNT(*) FILTER (WHERE status = 'FAILED') AS failed,
                       COUNT(*) AS total
                FROM core.t_event_outbox
                WHERE created_at >= ?
                GROUP BY created_at::date
                ORDER BY day
                """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, from);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date", r.get("day").toString());
            entry.put("direct", r.get("direct"));
            entry.put("retried", r.get("retried"));
            entry.put("failed", r.get("failed"));
            entry.put("total", r.get("total"));
            result.add(entry);
        }
        return result;
    }

    public List<Map<String, Object>> retriedRows(int limit) {
        String sql = """
                SELECT outbox_id, event_type, event_id, attempts,
                       created_at, published_at, last_error
                FROM core.t_event_outbox
                WHERE status = 'PUBLISHED' AND attempts > 1
                ORDER BY published_at DESC
                LIMIT ?
                """;
        return jdbcTemplate.queryForList(sql, limit);
    }

    public List<Map<String, Object>> failedRows(int limit) {
        String sql = """
                SELECT outbox_id, event_type, event_id, attempts,
                       created_at, failed_at, last_error
                FROM core.t_event_outbox
                WHERE status = 'FAILED'
                ORDER BY failed_at DESC
                LIMIT ?
                """;
        return jdbcTemplate.queryForList(sql, limit);
    }
}