package com.nexus.cms.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Acknowledgement of a read-state change.
 * <p>
 * Carries the updated unread count so the browser does not have to ask for it
 * separately after marking something read.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationAckDto {

    /**
     * The notification that was marked read, or null when every unread
     * notification was marked read.
     */
    private Long notificationId;

    /**
     * The unread count after the change.
     */
    private long unreadCount;
}
