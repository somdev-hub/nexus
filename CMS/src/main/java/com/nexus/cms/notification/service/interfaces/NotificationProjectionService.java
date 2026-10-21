package com.nexus.cms.notification.service.interfaces;

import com.nexus.cms.notification.entities.Notification;
import com.nexus.cms.notification.model.DomainEventDto;
import com.nexus.cms.notification.model.NotificationMessageDto;
import com.nexus.cms.notification.model.NotificationSnapshotDto;

/**
 * Projects a domain event into a stored notification, and owns the read state
 * of a user's inbox.
 */
public interface NotificationProjectionService {

    /**
     * Project a domain event into a notification.
     * <p>
     * Returns null when the event does not produce a notification — because
     * the policy does not consider the transition worth telling anybody about,
     * because the event has already been projected, or because the event
     * cannot be addressed to an organisation.
     *
     * @param event the domain event
     * @return the stored notification, or null
     */
    Notification project(DomainEventDto event);

    /**
     * The current inbox of a user.
     *
     * @param userId   the user
     * @param orgId    the organisation the user is acting in
     * @param maxItems the maximum number of notifications to return
     * @return the snapshot
     */
    NotificationSnapshotDto snapshot(Long userId, Long orgId, int maxItems);

    /**
     * Mark one notification as read for a user.
     *
     * @param notificationId the notification
     * @param userId         the user
     * @param orgId          the organisation the notification must belong to
     * @return the unread count after the change
     */
    long markRead(Long notificationId, Long userId, Long orgId);

    /**
     * Mark every unread notification as read for a user.
     *
     * @param userId the user
     * @param orgId  the organisation
     * @return the unread count after the change
     */
    long markAllRead(Long userId, Long orgId);

    /**
     * Convert a stored notification into the payload the browser receives.
     *
     * @param notification the notification
     * @return the payload
     */
    NotificationMessageDto toMessageDto(Notification notification);
}
