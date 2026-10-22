package com.nexus.core.event.transport;

/**
 * Publishes a domain event to its transport.
 */
public interface EventTransport {

    /**
     * Publish a domain event.
     *
     * @param event the event
     * @throws Exception when the publish fails
     */
    void publish(com.nexus.core.event.DomainEvent event) throws Exception;
}
