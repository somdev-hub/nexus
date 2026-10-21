package com.nexus.cms.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The current state of a user's inbox, sent in reply to a sync request.
 * <p>
 * The snapshot is the answer to "what have I missed?". It is sent over the
 * private queue because the unread count is per user: one user marking a
 * notification read must not clear it for everybody else.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSnapshotDto {

    /**
     * The most recent notifications, newest first, bounded by
     * {@code notification.snapshot.max-items}.
     */
    private List<NotificationMessageDto> items;

    /**
     * How many notifications the user has not read.
     */
    private long unreadCount;

    /**
     * Server time, as an ISO-8601 instant, so the browser can detect clock
     * drift and so that two snapshots can be ordered.
     * <p>
     * A string rather than an {@code Instant} — see
     * {@link DomainEventDto#occurredAt} for why.
     */
    private String serverTime;
}
