package com.nexus.cms.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.cms.notification.model.DomainEventDto;
import com.nexus.cms.notification.properties.NotificationConstants;
import com.nexus.cms.notification.service.interfaces.NotificationEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * The Kafka adapter for the domain event topic.
 * <p>
 * The listener does one thing: turn the bytes on the wire into a
 * {@link DomainEventDto} and hand it to the business logic. Everything that
 * decides *whether* and *what* is a notification lives in the service layer.
 * <p>
 * It runs on its own container factory so that the notification subsystem is
 * insulated from the chat and mail listeners: a failure here retries and
 * then lands in {@code core.domain.events-dlq} without touching anything
 * else in the application.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationEventService eventService;

    @KafkaListener(topics = NotificationConstants.EVENTS_TOPIC,
            groupId = NotificationConstants.PROJECTION_GROUP_ID,
            containerFactory = "notificationKafkaListenerContainerFactory")
    public void consume(String rawEvent) {
        if (rawEvent == null || rawEvent.isBlank()) {
            log.warn("Received an empty event — nothing to do");
            return;
        }

        DomainEventDto event;
        try {
            event = objectMapper.readValue(rawEvent, DomainEventDto.class);
        } catch (Exception e) {
            // A payload that cannot be parsed is a poison message. Let it
            // fail so the error handler routes it to the dead letter topic
            // rather than dropping it on the floor.
            log.error("Could not parse domain event — it will be routed to the dead letter topic", e);
            throw new IllegalStateException("Could not parse domain event: " + e.getMessage(), e);
        }

        eventService.handle(event);
    }
}
