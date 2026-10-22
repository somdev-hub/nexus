package com.nexus.core.event.publisher;

/**
 * Publishes a domain event.
 * <p>
 * The implementation is free to write it to an outbox, to a bus, or to a
 * remote system. The publisher of the event — the code that decides
 * <em>what</em> happened — does not need to know which one is in use.
 */
public interface DomainEventPublisher {

    /**
     * Publish a domain event.
     *
     * @param event the event
     */
    void publish(com.nexus.core.event.DomainEvent event);
}
