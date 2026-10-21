package com.nexus.cms.notification.service.interfaces;

import com.nexus.cms.notification.model.NotificationMessageDto;

/**
 * Relays a notification between CMS instances.
 * <p>
 * The STOMP broker is in-memory and lives inside a single JVM. A client
 * connected to instance A will not see a notification that instance B
 * produced, so the notification is published to Redis and every instance
 * relays it to the clients it holds.
 */
public interface NotificationRedisPublisher {

    /**
     * Publish a notification for an organisation.
     *
     * @param orgId   the organisation
     * @param message the notification
     */
    void publishToOrg(Long orgId, NotificationMessageDto message);
}
