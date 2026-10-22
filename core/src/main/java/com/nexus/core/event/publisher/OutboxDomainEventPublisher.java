package com.nexus.core.event.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.core.event.DomainEvent;
import com.nexus.core.event.outbox.EventOutbox;
import com.nexus.core.event.outbox.EventOutboxRepo;
import com.nexus.core.event.outbox.OutboxStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Writes a domain event into the outbox.
 * <p>
 * The write joins the caller's transaction, so the event is committed or
 * rolled back together with the business change it describes. A failure to
 * serialise the event does not lose it: a minimal row is still written so
 * that the relay has something to retry and the failure is visible.
 */
@Service
public class OutboxDomainEventPublisher implements com.nexus.core.event.publisher.DomainEventPublisher {

    private final EventOutboxRepo outboxRepo;
    private final ObjectMapper objectMapper;

    public OutboxDomainEventPublisher(EventOutboxRepo outboxRepo, ObjectMapper objectMapper) {
        this.outboxRepo = outboxRepo;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(DomainEvent event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            // A serialisation failure must not lose the event. Write a minimal
            // row carrying the identifiers so that the relay can retry and the
            // failure is visible.
            logSerialisationFailure(e);
            payload = minimalPayload(event);
        }
        EventOutbox row = EventOutbox.pending(event.eventType().name(), payload);
        row.setEventId(event.eventId());
        outboxRepo.save(row);
    }

    private void logSerialisationFailure(Exception e) {
        System.err.println("[Outbox] Failed to serialise domain event: " + e.getMessage());
    }

    private String minimalPayload(DomainEvent event) {
        return "{\"eventId\":\"" + event.eventId() + "\",\"eventType\":\"" + event.eventType().name()
                + "\",\"entityType\":\"" + event.entityType() + "\",\"entityId\":" + event.entityId() + "}";
    }
}
