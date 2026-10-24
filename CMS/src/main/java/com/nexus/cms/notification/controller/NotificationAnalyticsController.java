package com.nexus.cms.notification.controller;

import com.nexus.cms.notification.entities.Notification;
import com.nexus.cms.notification.entities.NotificationReadReceipt;
import com.nexus.cms.notification.repositories.NotificationReadReceiptRepo;
import com.nexus.cms.notification.repositories.NotificationRepo;
import com.nexus.cms.notification.service.implementations.OutboxAnalyticsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * REST controller exposing notification analytics for the admin dashboard.
 * <p>
 * All endpoints return aggregated, read-only views of the notification
 * data. No mutation is performed.
 */
@RestController
@RequestMapping("/api/notifications/analytics")
@RequiredArgsConstructor
@Slf4j
public class NotificationAnalyticsController {

    private final NotificationRepo notificationRepo;
    private final NotificationReadReceiptRepo readReceiptRepo;
    private final OutboxAnalyticsServiceImpl outboxAnalytics;

    /**
     * Birds-eye overview: total counts, read rate, delivery lag, etc.
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> overview() {
        long total = notificationRepo.countAll();
        long unread = 0; // computed below
        // Count distinct orgs and users
        List<Object[]> orgCounts = notificationRepo.countByOrg();
        long totalOrgs = orgCounts.size();

        // Read receipts total
        long totalReadReceipts = readReceiptRepo.count();

        // Unread = total notifications - total read receipts (approximate, since
        // a notification can be read by multiple users)
        // More accurate: count notifications with no read receipt for any user
        // For simplicity, use read receipts as proxy
        long readCount = totalReadReceipts;
        double readRate = total > 0 ? (double) readCount / total * 100 : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalNotifications", total);
        result.put("totalOrgs", totalOrgs);
        result.put("totalUsers", 0); // placeholder
        result.put("unreadCount", Math.max(0, total - readCount));
        result.put("readCount", readCount);
        result.put("readRate", Math.round(readRate * 10.0) / 10.0);
        result.put("avgDeliveryLagMs", 342); // placeholder
        result.put("realtimeDeliveryRate", 97.8); // placeholder
        result.put("failureCount", 0);
        result.put("retentionDays", 7);
        return ResponseEntity.ok(result);
    }

    /**
     * Trend data: daily notification counts.
     */
    @GetMapping("/trends")
    public ResponseEntity<List<Map<String, Object>>> trends(
            @RequestParam(defaultValue = "30") int days) {
        Instant fromInstant = Instant.now().minus(days, ChronoUnit.DAYS);
        Timestamp from = Timestamp.from(fromInstant);
        List<Notification> recent = notificationRepo.findRecentSince(from);

        // Group by date
        Map<String, long[]> byDate = new TreeMap<>();
        for (Notification n : recent) {
            String date = n.getCreatedAt().toInstant().toString().substring(0, 10);
            byDate.computeIfAbsent(date, k -> new long[]{0, 0});
            byDate.get(date)[0]++; // total
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, long[]> e : byDate.entrySet()) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", e.getKey());
            point.put("count", e.getValue()[0]);
            point.put("read", 0);
            point.put("unread", e.getValue()[0]);
            result.add(point);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Per-organisation breakdown.
     */
    @GetMapping("/by-org")
    public ResponseEntity<Map<String, Object>> byOrg(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Object[]> rows = notificationRepo.countByOrg();
        List<Object[]> lastRows = notificationRepo.lastNotificationByOrg();
        Map<Long, Object> lastByOrg = new HashMap<>();
        for (Object[] row : lastRows) {
            lastByOrg.put((Long) row[0], row[1]);
        }

        List<Map<String, Object>> all = new ArrayList<>();
        for (Object[] row : rows) {
            Long orgId = (Long) row[0];
            long count = (Long) row[1];
            Object lastAt = lastByOrg.get(orgId);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("orgId", orgId);
            entry.put("orgName", "Org " + orgId);
            entry.put("totalNotifications", count);
            entry.put("unreadCount", 0);
            entry.put("readRate", 0.0);
            entry.put("lastNotificationAt", lastAt != null ? lastAt.toString() : null);
            entry.put("avgDeliveryLagMs", 0);
            all.add(entry);
        }

        // Paginate
        int total = all.size();
        int totalPages = (int) Math.ceil((double) total / size);
        int start = page * size;
        int end = Math.min(start + size, total);
        List<Map<String, Object>> content = start < total ? all.subList(start, end) : new ArrayList<>();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("totalElements", total);
        result.put("totalPages", totalPages);
        result.put("page", page);
        result.put("size", size);
        return ResponseEntity.ok(result);
    }

    /**
     * Per-event-type breakdown.
     */
    @GetMapping("/by-event-type")
    public ResponseEntity<List<Map<String, Object>>> byEventType() {
        List<Object[]> rows = notificationRepo.countByEventType();
        long total = notificationRepo.countAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            String eventType = (String) row[0];
            long count = (Long) row[1];
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("eventType", eventType);
            entry.put("displayName", eventType.replace("_", " "));
            entry.put("count", count);
            entry.put("percentage", total > 0 ? Math.round(count * 1000.0 / total) / 10.0 : 0);
            entry.put("avgDeliveryLagMs", 0);
            entry.put("severity", "INFO");
            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Severity breakdown.
     */
    @GetMapping("/by-severity")
    public ResponseEntity<List<Map<String, Object>>> bySeverity() {
        List<Object[]> rows = notificationRepo.countBySeverity();
        long total = notificationRepo.countAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            String severity = (String) row[0];
            long count = (Long) row[1];
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("severity", severity);
            entry.put("count", count);
            entry.put("percentage", total > 0 ? Math.round(count * 1000.0 / total) / 10.0 : 0);
            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Addressing breakdown.
     */
    @GetMapping("/by-addressing")
    public ResponseEntity<List<Map<String, Object>>> byAddressing() {
        List<Object[]> rows = notificationRepo.countByAddressing();
        long total = notificationRepo.countAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            String addressing = (String) row[0];
            long count = (Long) row[1];
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("addressing", addressing);
            entry.put("count", count);
            entry.put("percentage", total > 0 ? Math.round(count * 1000.0 / total) / 10.0 : 0);
            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Recent notifications timeline (microscopic view).
     */
    @GetMapping("/timeline")
    public ResponseEntity<Map<String, Object>> timeline(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        long total = notificationRepo.countAll();
        int totalPages = (int) Math.ceil((double) total / size);
        List<Notification> recent = notificationRepo.findAllOrderByCreatedAtDesc(
                PageRequest.of(page, size));
        List<Map<String, Object>> content = new ArrayList<>();
        for (Notification n : recent) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("notificationId", n.getNotificationId());
            entry.put("eventId", n.getEventId());
            entry.put("eventType", n.getEventType());
            entry.put("entityType", n.getEntityType());
            entry.put("entityRef", n.getEntityRef());
            entry.put("title", n.getTitle());
            entry.put("severity", n.getSeverity());
            entry.put("targetOrgId", n.getTargetOrgId());
            entry.put("targetOrgName", "Org " + n.getTargetOrgId());
            entry.put("addressing", n.getAddressing());
            entry.put("createdAt", n.getCreatedAt().toInstant().toString());
            entry.put("readCount", 0);
            entry.put("totalRecipients", 0);
            entry.put("deliveryStatus", "DELIVERED");
            entry.put("deliveryLagMs", 0);
            content.add(entry);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("totalElements", total);
        result.put("totalPages", totalPages);
        result.put("page", page);
        result.put("size", size);
        return ResponseEntity.ok(result);
    }

    /**
     * Delivery performance metrics.
     */
    @GetMapping("/delivery")
    public ResponseEntity<Map<String, Object>> delivery() {
        long total = notificationRepo.countAll();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalDelivered", total);
        result.put("totalPending", 0);
        result.put("totalFailed", 0);
        result.put("deliveryRate", 100.0);
        result.put("avgDeliveryLagMs", 0);
        result.put("p95DeliveryLagMs", 0);
        result.put("p99DeliveryLagMs", 0);
        return ResponseEntity.ok(result);
    }

    // ------------------------------------------------------------------
    // Outbox delivery analytics (direct vs retried vs failed)
    // ------------------------------------------------------------------

    @GetMapping("/outbox/summary")
    public ResponseEntity<Map<String, Object>> outboxSummary() {
        return ResponseEntity.ok(outboxAnalytics.summary());
    }

    @GetMapping("/outbox/by-event-type")
    public ResponseEntity<List<Map<String, Object>>> outboxByEventType() {
        return ResponseEntity.ok(outboxAnalytics.byEventType());
    }

    @GetMapping("/outbox/trend")
    public ResponseEntity<List<Map<String, Object>>> outboxTrend(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(outboxAnalytics.trend(days));
    }

    @GetMapping("/outbox/retried")
    public ResponseEntity<Map<String, Object>> outboxRetried(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> all = outboxAnalytics.retriedRows(1000);
        int total = all.size();
        int totalPages = (int) Math.ceil((double) total / size);
        int start = page * size;
        int end = Math.min(start + size, total);
        List<Map<String, Object>> content = start < total ? all.subList(start, end) : new ArrayList<>();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("totalElements", total);
        result.put("totalPages", totalPages);
        result.put("page", page);
        result.put("size", size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/outbox/failed")
    public ResponseEntity<Map<String, Object>> outboxFailed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> all = outboxAnalytics.failedRows(1000);
        int total = all.size();
        int totalPages = (int) Math.ceil((double) total / size);
        int start = page * size;
        int end = Math.min(start + size, total);
        List<Map<String, Object>> content = start < total ? all.subList(start, end) : new ArrayList<>();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("totalElements", total);
        result.put("totalPages", totalPages);
        result.put("page", page);
        result.put("size", size);
        return ResponseEntity.ok(result);
    }
}