package com.nexus.core.event.transport;

import com.nexus.core.event.DomainEvent;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Publishes a domain event to Kafka.
 * <p>
 * The message key is the target organisation id, so that every event for one
 * organisation lands on the same partition and the per-organisation ordering
 * of the stream is preserved. The topic is owned by {@code nexus/CMS}, which
 * creates it on start-up; this producer only publishes to it.
 */
public class KafkaEventTransport implements EventTransport {

    /**
     * The topic carrying domain events published by {@code nexus/core}.
     */
    public static final String TOPIC = "core.domain.events";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public KafkaEventTransport(KafkaTemplate<String, Object> kafkaTemplate,
                              com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(DomainEvent event) throws Exception {
        String payload = objectMapper.writeValueAsString(event);
        String key = event.targetOrgId() != null ? event.targetOrgId().toString()
                : (event.buyerOrgId() != null ? event.buyerOrgId().toString() : event.eventId());
        kafkaTemplate.send(TOPIC, key, payload).get(10, TimeUnit.SECONDS);
    }
}
