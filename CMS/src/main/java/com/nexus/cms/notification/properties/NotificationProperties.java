package com.nexus.cms.notification.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration for the notification subsystem.
 * <p>
 * All of it is driven from {@code application.properties} under the
 * {@code notification.*} prefix, so nothing here needs a code change to be
 * tuned.
 */
@Getter
@Setter
@Component
public class NotificationProperties {

    /**
     * How a notification reaches the browser.
     * <ul>
     *   <li>{@code REDIS} — the consumer publishes to Redis and every CMS
     *   instance relays it to the clients connected to it. Correct with any
     *   number of instances. This is the default.</li>
     *   <li>{@code DIRECT} — the consumer pushes straight to the broker. Only
     *   correct with a single CMS instance: with more than one, a client
     *   connected to another instance never sees the notification, and the
     *   loss is silent.</li>
     * </ul>
     */
    @Value("${notification.relay.mode:REDIS}")
    private String relayMode;

    /**
     * How many days a notification is kept. Zero disables the cleanup job;
     * a negative value keeps notifications forever.
     */
    @Value("${notification.retention.days:7}")
    private int retentionDays;

    /**
     * How many notifications a sync snapshot returns.
     */
    @Value("${notification.snapshot.max-items:50}")
    private int snapshotMaxItems;

    /**
     * Longest rendered body, in characters. Anything longer is truncated.
     */
    @Value("${notification.policy.max-body-length:500}")
    private int maxBodyLength;

    /**
     * Whether failed events are recorded in {@code cms.t_notification_failures}
     * for inspection and replay.
     */
    @Value("${notification.dlq.tracker.enabled:true}")
    private boolean dlqTrackerEnabled;

    /**
     * Whether failed events are automatically replayed.
     */
    @Value("${notification.dlq.replay.enabled:true}")
    private boolean dlqReplayEnabled;

    /**
     * How many times a failed event may be replayed before it is given up on.
     */
    @Value("${notification.dlq.replay.max-replays:3}")
    private int dlqReplayMaxReplays;

    /**
     * How long a failed event waits before it is replayed.
     */
    @Value("${notification.dlq.replay.cooldown-seconds:60}")
    private long dlqReplayCooldownSeconds;

    /**
     * How many days a failed event is kept for inspection.
     */
    @Value("${notification.dlq.failure-retention.days:30}")
    private int dlqFailureRetentionDays;

    /**
     * Whether notifications are relayed through Redis.
     */
    public boolean isRedisRelay() {
        return !"DIRECT".equalsIgnoreCase(relayMode);
    }
}
