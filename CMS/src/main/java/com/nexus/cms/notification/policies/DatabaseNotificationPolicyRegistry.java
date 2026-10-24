package com.nexus.cms.notification.policies;

// PolicyEntity = com.nexus.cms.notification.entities.NotificationPolicy (use fully qualified name)
import com.nexus.cms.notification.repositories.NotificationPolicyRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Database-backed notification policy registry.
 * <p>
 * Loads all active rules from {@code cms.t_notification_policy} on startup
 * and caches them in memory. The projector reads from the cache (no DB hit
 * per event). CRUD operations call {@link #refresh()} to reload.
 * <p>
 * Replaces {@link InMemoryNotificationPolicyRegistry}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
public class DatabaseNotificationPolicyRegistry implements NotificationPolicyRegistry, ApplicationRunner {

    private final NotificationPolicyRepo repo;

    /**
     * Cache: key = "eventType|previousStatus->newStatus" or "eventType|newStatus"
     */
    private final Map<String, NotificationPolicy> byTransition = new ConcurrentHashMap<>();
    private final Map<String, NotificationPolicy> byTargetStatus = new ConcurrentHashMap<>();

    /**
     * Load all active policies from the database.
     */
    @Transactional(readOnly = true)
    public void refresh() {
        byTransition.clear();
        byTargetStatus.clear();

        List<com.nexus.cms.notification.entities.NotificationPolicy> policies = repo.findByIsActiveTrue();
        for (com.nexus.cms.notification.entities.NotificationPolicy p : policies) {
            NotificationPolicy policy = toPolicy(p);
            if (p.getPreviousStatus() != null && !p.getPreviousStatus().isBlank()) {
                byTransition.put(transitionKey(p.getEventType(), p.getPreviousStatus(), p.getNewStatus()), policy);
            } else {
                byTargetStatus.put(targetKey(p.getEventType(), p.getNewStatus()), policy);
            }
        }
        log.info("Notification policy registry refreshed — {} transition rules, {} target-status rules",
                byTransition.size(), byTargetStatus.size());
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        refresh();
    }

    @Override
    public Optional<NotificationPolicy> findForTransition(String eventType, String previousStatus, String newStatus) {
        if (newStatus == null) {
            return Optional.empty();
        }
        // Exact transition wins
        if (previousStatus != null && eventType != null) {
            NotificationPolicy exact = byTransition.get(transitionKey(eventType, previousStatus, newStatus));
            if (exact != null) {
                return Optional.of(exact);
            }
        }
        // Target status fallback
        if (eventType != null) {
            NotificationPolicy target = byTargetStatus.get(targetKey(eventType, newStatus));
            if (target != null) {
                return Optional.of(target);
            }
        }
        // Backwards compat: try without event type
        if (previousStatus != null) {
            NotificationPolicy exact = byTransition.get(transitionKey(null, previousStatus, newStatus));
            if (exact != null) {
                return Optional.of(exact);
            }
        }
        return Optional.ofNullable(byTargetStatus.get(targetKey(null, newStatus)));
    }

    /**
     * Convert a JPA entity to a policy object.
     */
    private com.nexus.cms.notification.policies.NotificationPolicy toPolicy(com.nexus.cms.notification.entities.NotificationPolicy entity) {
        return com.nexus.cms.notification.policies.NotificationPolicy.of(
                entity.getSeverity(),
                entity.getTitleTemplate(),
                entity.getBodyTemplate(),
                entity.getAddressTo()
        );
    }

    private static String transitionKey(String eventType, String previousStatus, String newStatus) {
        return (eventType != null ? eventType : "*") + "|" + previousStatus + "->" + newStatus;
    }

    private static String targetKey(String eventType, String newStatus) {
        return (eventType != null ? eventType : "*") + "|" + newStatus;
    }
}