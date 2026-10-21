package com.nexus.cms.notification.service.implementations;

import com.nexus.cms.notification.entities.Notification;
import com.nexus.cms.notification.entities.NotificationReadReceipt;
import com.nexus.cms.notification.properties.NotificationMasterCodes;
import com.nexus.cms.notification.model.DomainEventDto;
import com.nexus.cms.notification.model.NotificationMessageDto;
import com.nexus.cms.notification.model.NotificationSnapshotDto;
import com.nexus.cms.notification.policies.NotificationPolicy;
import com.nexus.cms.notification.policies.NotificationPolicyRegistry;
import com.nexus.cms.notification.properties.NotificationProperties;
import com.nexus.cms.notification.repositories.NotificationReadReceiptRepo;
import com.nexus.cms.notification.repositories.NotificationRepo;
import com.nexus.cms.notification.service.interfaces.NotificationProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProjectionServiceImpl implements NotificationProjectionService {

    /**
     * How many notifications are examined when marking everything as read.
     * Bounded by the retention window, so this is comfortably large enough.
     */
    private static final int MARK_ALL_READ_LIMIT = 1000;

    private final NotificationRepo notificationRepo;
    private final NotificationReadReceiptRepo readReceiptRepo;
    private final NotificationPolicyRegistry policyRegistry;
    private final NotificationProperties properties;

    @Override
    @Transactional
    public Notification project(DomainEventDto event) {
        if (event == null) {
            return null;
        }
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            log.warn("Ignoring event without an eventId — it cannot be deduplicated");
            return null;
        }

        // Already projected? The producer publishes at-least-once, so this is
        // the normal path for a redelivered event.
        if (notificationRepo.findByEventId(event.getEventId()).isPresent()) {
            log.debug("Event {} has already been projected — skipping", event.getEventId());
            return null;
        }

        Map<String, Object> facts = factsOf(event);
        String previousStatus = stringFact(facts, "previousStatus");
        String newStatus = stringFact(facts, "newStatus");

        NotificationPolicy policy = policyRegistry.findForTransition(previousStatus, newStatus).orElse(null);
        if (policy == null) {
            log.debug("No notification policy for transition {} -> {} — event {} is not notifiable",
                    previousStatus, newStatus, event.getEventId());
            return null;
        }
        if (!policy.persist()) {
            log.debug("Policy for transition {} -> {} is not persistent — event {} is not stored",
                    previousStatus, newStatus, event.getEventId());
            return null;
        }

        Long targetOrgId = resolveTargetOrg(event, policy);
        if (targetOrgId == null) {
            log.warn("Event {} cannot be addressed to an organisation — skipping", event.getEventId());
            return null;
        }

        Map<String, Object> templateValues = templateValues(event, facts);
        Notification notification = new Notification();
        notification.setEventId(event.getEventId());
        notification.setEventType(event.getEventType());
        notification.setTargetOrgId(targetOrgId);
        notification.setEntityType(event.getEntityType());
        notification.setEntityId(event.getEntityId());
        notification.setEntityRef(event.getEntityRef());
        notification.setTitle(truncate(policy.renderTitle(templateValues), 500));
        notification.setBody(truncate(policy.renderBody(templateValues), properties.getMaxBodyLength()));
        notification.setSeverity(policy.severity());
        notification.setAddressing(policy.addressing());
        notification.setPayload(new HashMap<>(facts));
        notification.setCreatedAt(Timestamp.from(occurredAtOf(event)));

        try {
            Notification saved = notificationRepo.save(notification);
            log.info("Projected event {} into notification {} for org {} — '{}'",
                    event.getEventId(), saved.getNotificationId(), targetOrgId, saved.getTitle());
            return saved;
        } catch (DataIntegrityViolationException e) {
            // Two consumers raced on the same event. The unique constraint on
            // event_id settles it — one of them wins, the other backs off.
            log.debug("Event {} was projected concurrently — skipping duplicate", event.getEventId());
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationSnapshotDto snapshot(Long userId, Long orgId, int maxItems) {
        if (userId == null || orgId == null) {
            return NotificationSnapshotDto.builder()
                    .items(List.of())
                    .unreadCount(0)
                    .serverTime(iso(Instant.now()))
                    .build();
        }

        int limit = maxItems > 0 ? maxItems : properties.getSnapshotMaxItems();
        List<Notification> notifications = notificationRepo.findRecentListForOrg(orgId, PageRequest.of(0, limit));

        List<NotificationMessageDto> items = new ArrayList<>(notifications.size());
        for (Notification notification : notifications) {
            items.add(toMessageDto(notification));
        }

        return NotificationSnapshotDto.builder()
                .items(items)
                .unreadCount(notificationRepo.countUnreadForUserInOrg(orgId, userId))
                .serverTime(iso(Instant.now()))
                .build();
    }

    @Override
    @Transactional
    public long markRead(Long notificationId, Long userId, Long orgId) {
        if (notificationId == null || userId == null || orgId == null) {
            return 0;
        }
        // The notification must belong to the organisation the user is acting
        // in, otherwise a user could mark another organisation's notification
        // read by guessing its id.
        Notification notification = notificationRepo
                .findByNotificationIdAndTargetOrgId(notificationId, orgId)
                .orElse(null);
        if (notification == null) {
            log.warn("User {} tried to mark notification {} which does not belong to org {}",
                    userId, notificationId, orgId);
            return notificationRepo.countUnreadForUserInOrg(orgId, userId);
        }

        if (!readReceiptRepo.existsByNotificationNotificationIdAndUserId(notificationId, userId)) {
            NotificationReadReceipt receipt = new NotificationReadReceipt();
            receipt.setNotification(notification);
            receipt.setUserId(userId);
            receipt.setReadAt(Timestamp.from(Instant.now()));
            readReceiptRepo.save(receipt);
        }
        return notificationRepo.countUnreadForUserInOrg(orgId, userId);
    }

    @Override
    @Transactional
    public long markAllRead(Long userId, Long orgId) {
        if (userId == null || orgId == null) {
            return 0;
        }
        List<Notification> unread = notificationRepo.findUnreadForUserInOrg(orgId, userId,
                PageRequest.of(0, MARK_ALL_READ_LIMIT));
        if (unread.isEmpty()) {
            return 0;
        }

        Timestamp now = Timestamp.from(Instant.now());
        for (Notification notification : unread) {
            NotificationReadReceipt receipt = new NotificationReadReceipt();
            receipt.setNotification(notification);
            receipt.setUserId(userId);
            receipt.setReadAt(now);
            readReceiptRepo.save(receipt);
        }
        log.info("User {} marked {} notifications as read in org {}", userId, unread.size(), orgId);
        return notificationRepo.countUnreadForUserInOrg(orgId, userId);
    }

    /**
     * When the event happened.
     * <p>
     * The wire format is an ISO-8601 string. A value that cannot be parsed is
     * treated as "now" rather than failing the whole event — the notification
     * is still worth delivering, it is just dated now.
     */
    private Instant occurredAtOf(DomainEventDto event) {
        String occurredAt = event.getOccurredAt();
        if (occurredAt == null || occurredAt.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(occurredAt);
        } catch (Exception e) {
            try {
                return java.time.OffsetDateTime.parse(occurredAt).toInstant();
            } catch (Exception ignored) {
                log.warn("Event {} carries an unparsable occurredAt '{}' — the notification is dated now",
                        event.getEventId(), occurredAt);
                return Instant.now();
            }
        }
    }

    /**
     * Format an instant for the wire.
     */
    private static String iso(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    /**
     * Work out which organisation the notification is for.
     */
    private Long resolveTargetOrg(DomainEventDto event, NotificationPolicy policy) {
        String addressTo = policy.addressTo();
        if (addressTo == null) {
            addressTo = NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY;
        }
        return switch (addressTo) {
            case NotificationMasterCodes.ADDRESS_TO_ACTOR_ORG -> event.getActorOrgId();
            case NotificationMasterCodes.ADDRESS_TO_RETAILER -> event.getBuyerOrgId();
            case NotificationMasterCodes.ADDRESS_TO_SUPPLIER -> event.getSupplierOrgId();
            // COUNTERPARTY, and the default for anything unknown.
            default -> {
                // The counterparty of whoever acted. When the actor is unknown
                // — a system job, for instance — the event is treated as
                // having been performed by the buyer, which is the correct
                // reading for the transitions a system performs.
                Long actor = event.getActorOrgId() != null
                        ? event.getActorOrgId()
                        : event.getBuyerOrgId();
                if (Objects.equals(actor, event.getBuyerOrgId())) {
                    yield event.getSupplierOrgId();
                }
                yield event.getBuyerOrgId();
            }
        };
    }

    /**
     * The values available to a notification template.
     */
    private Map<String, Object> templateValues(DomainEventDto event, Map<String, Object> facts) {
        Map<String, Object> values = new HashMap<>(facts);
        if (event.getEntityRef() != null) {
            // {poNumber} is the friendlier name for the entity reference.
            values.putIfAbsent("poNumber", event.getEntityRef());
        }
        values.putIfAbsent("entityRef", event.getEntityRef());
        values.putIfAbsent("entityType", event.getEntityType());
        values.putIfAbsent("entityId", event.getEntityId());
        return values;
    }

    private Map<String, Object> factsOf(DomainEventDto event) {
        return event.getFacts() != null ? event.getFacts() : Map.of();
    }

    private String stringFact(Map<String, Object> facts, String key) {
        Object value = facts.get(key);
        return value != null ? value.toString() : null;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (maxLength <= 0 || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    /**
     * Convert a stored notification into the payload the browser receives.
     */
    public NotificationMessageDto toMessageDto(Notification notification) {
        return NotificationMessageDto.builder()
                .notificationId(notification.getNotificationId())
                .eventId(notification.getEventId())
                .eventType(notification.getEventType())
                .entityType(notification.getEntityType())
                .entityId(notification.getEntityId())
                .entityRef(notification.getEntityRef())
                .title(notification.getTitle())
                .body(notification.getBody())
                .severity(notification.getSeverity())
                .targetOrgId(notification.getTargetOrgId())
                .facts(payloadAsMap(notification.getPayload()))                .createdAt(notification.getCreatedAt() != null
                        ? iso(notification.getCreatedAt().toInstant())
                        : null)
                .build();
    }

    private Map<String, Object> payloadAsMap(Map<String, Object> payload) {
        return payload != null ? payload : Map.of();
    }
}