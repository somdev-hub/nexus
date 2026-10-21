package com.nexus.cms.notification.service.interfaces;

import com.nexus.cms.notification.entities.Notification;

/**
 * Delivers a notification to its audience.
 * <p>
 * A router is a strategy: one implementation per addressing mode. The
 * delivery service picks the router that matches the policy, so adding
 * user-level or authority-level addressing later means adding one more
 * implementation and nothing else.
 */
public interface NotificationDeliveryRouter {

    /**
     * The addressing mode this router handles.
     * <p>
     * A code rather than an enum — the catalogue lives in
     * {@code cms.t_notification_addressing_type_master}.
     */
    String addressingType();

    /**
     * Deliver a notification to its audience.
     *
     * @param notification the notification to deliver
     */
    void deliver(Notification notification);
}
