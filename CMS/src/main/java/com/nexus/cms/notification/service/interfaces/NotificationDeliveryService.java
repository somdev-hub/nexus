package com.nexus.cms.notification.service.interfaces;

import com.nexus.cms.notification.entities.Notification;
import com.nexus.cms.notification.model.DomainEventDto;

/**
 * Delivers a projected notification to its audience.
 */
public interface NotificationDeliveryService {

    /**
     * Deliver a notification.
     * <p>
     * Delivery is best effort. The notification has already been committed by
     * the time this is called, so a failure here costs a live push, not the
     * notification itself — the user still finds it in their inbox.
     *
     * @param notification the notification to deliver
     * @param event        the event it was projected from
     */
    void deliver(Notification notification, DomainEventDto event);
}
