package com.nexus.cms.notification.service.interfaces;

import com.nexus.cms.notification.model.DomainEventDto;

/**
 * Handles a domain event that has arrived on the event topic.
 * <p>
 * This is the business logic behind the Kafka listener: the listener itself
 * only turns bytes into an event and hands it over.
 */
public interface NotificationEventService {

    /**
     * Handle one domain event.
     * <p>
     * An event whose type is not active is ignored. An event that has
     * already been projected is ignored. Neither is a failure.
     *
     * @param event the domain event
     */
    void handle(DomainEventDto event);
}
