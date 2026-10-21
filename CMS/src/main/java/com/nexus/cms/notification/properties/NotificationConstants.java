package com.nexus.cms.notification.properties;

/**
 * Constants for the notification subsystem
 * ({@code com.nexus.cms.notification}).
 * <p>
 * The notification subsystem consumes domain events published by
 * {@code nexus/core}, decides who should be told and what they should read,
 * persists the result, and delivers it in real time over the existing STOMP
 * broker.
 * <p>
 * <b>Destination isolation.</b> Everything the notification subsystem uses
 * lives under a dedicated namespace so that it can never collide with the
 * chat subsystem, which shares the same broker:
 * <pre>
 * chat           /topic/conversations/**        /user/{principal}/queue/messages
 * notification  /topic/nexus/notifications/**  /user/{principal}/queue/nexus/notifications
 * </pre>
 */
public final class NotificationConstants {

    private NotificationConstants() {
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Kafka
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Topic carrying domain events published by {@code nexus/core}.
     * <p>
     * The message key is the target organisation id, so every event for one
     * organisation lands on one partition and the per-organisation ordering of
     * the stream is preserved.
     */
    public static final String EVENTS_TOPIC = "core.domain.events";

    /**
     * Dead letter topic for {@link #EVENTS_TOPIC}. The suffix matches the
     * convention used by the shared error handler, which derives the dead
     * letter topic from the source topic name.
     */
    public static final String EVENTS_DLQ_TOPIC = "core.domain.events-dlq";

    /**
     * Consumer group that projects domain events into notifications.
     */
    public static final String PROJECTION_GROUP_ID = "cms-notification-projection";

    /**
     * Consumer group that records failed events for inspection and replay.
     * Separate from the projection group so that tracking never competes with
     * delivery.
     */
    public static final String DLQ_TRACKER_GROUP_ID = "cms-notification-dlq-tracker";

    // ─────────────────────────────────────────────────────────────────────────
    // STOMP destinations
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Prefix of the organisation broadcast topic. The organisation id is
     * appended: {@code /topic/nexus/notifications/org/123}.
     */
    public static final String ORG_TOPIC_PREFIX = "/topic/nexus/notifications/org/";

    /**
     * Private destination used with {@code convertAndSendToUser}. Spring
     * resolves it to {@code /user/<principal>/queue/nexus/notifications}.
     */
    public static final String USER_QUEUE_DESTINATION = "/queue/nexus/notifications";

    /**
     * The destination the browser subscribes to for the private queue.
     */
    public static final String CLIENT_USER_QUEUE = "/user" + USER_QUEUE_DESTINATION;

    /**
     * Prefix of the application destinations the browser may send to. Spring
     * routes {@code /app/nexus/notifications/ws/...} to the matching
     * {@code @MessageMapping}.
     */
    public static final String APP_PREFIX = "/app/nexus/notifications/ws/";

    /**
     * Application destination that asks the server for the current inbox
     * snapshot.
     */
    public static final String APP_SYNC = APP_PREFIX + "sync";

    /**
     * Application destination that marks one notification as read.
     */
    public static final String APP_MARK_READ = APP_PREFIX + "mark-read";

    /**
     * Application destination that marks every unread notification as read.
     */
    public static final String APP_MARK_ALL_READ = APP_PREFIX + "mark-all-read";

    // ─────────────────────────────────────────────────────────────────────────
    // Redis
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Prefix of the Redis pub/sub channel used to relay a notification from
     * the instance that consumed the event to the instance the client is
     * connected to. The organisation id is appended.
     */
    public static final String REDIS_CHANNEL_PREFIX = "notification:org:";

    /**
     * Pattern subscribed to by the notification relay container.
     */
    public static final String REDIS_CHANNEL_PATTERN = REDIS_CHANNEL_PREFIX + "*";

    // ─────────────────────────────────────────────────────────────────────────
    // Protocol
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Header carrying the organisation the client is currently acting in.
     * <p>
     * The same header is used on REST calls. It is necessary because the
     * organisation carried by the token is the user's <em>home</em>
     * organisation, which is not necessarily the one they are acting in.
     */
    public static final String ORGANIZATION_ID_HEADER = "X-Organization-ID";

    /**
     * Header carrying the number of times an event has been replayed.
     */
    public static final String REPLAY_COUNT_HEADER = "x-replay-count";

    // ─────────────────────────────────────────────────────────────────────────
    // Purchase order status names
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Purchase order status names as they appear in the domain event.
     * <p>
     * These mirror {@code com.nexus.core.model.enums.PurchaseOrderStatus}.
     * They are repeated here as plain strings so that CMS does not need a
     * compile-time dependency on the core module, and so that the event
     * contract stays a versioned wire format rather than a Java type.
     */
    public static final String PO_STATUS_DRAFT = "DRAFT";
    public static final String PO_STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String PO_STATUS_APPROVED = "APPROVED";
    public static final String PO_STATUS_REJECTED = "REJECTED";
    public static final String PO_STATUS_SENT_TO_SUPPLIER = "SENT_TO_SUPPLIER";
    public static final String PO_STATUS_ACKNOWLEDGED = "ACKNOWLEDGED";
    public static final String PO_STATUS_AWAITING_PICKUP = "AWAITING_PICKUP";
    public static final String PO_STATUS_PICKED_UP = "PICKED_UP";
    public static final String PO_STATUS_PARTIALLY_RECEIVED = "PARTIALLY_RECEIVED";
    public static final String PO_STATUS_RECEIVED = "RECEIVED";
    public static final String PO_STATUS_INVOICED = "INVOICED";
    public static final String PO_STATUS_PAID = "PAID";
    public static final String PO_STATUS_CANCELLED = "CANCELLED";
    public static final String PO_STATUS_CLOSED = "CLOSED";
}
