package com.nexus.cms.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.cms.notification.model.DomainEventDto;
import com.nexus.cms.notification.properties.NotificationConstants;
import com.nexus.cms.notification.service.interfaces.NotificationFailureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Records the events that could not be projected.
 * <p>
 * The shared error handler routes a record it cannot process to
 * {@code core.domain.events-dlq}. This listener picks it up from there and
 * writes it to {@code cms.t_notification_failures}, which is the queryable
 * record of what is broken — a Kafka topic cannot be queried.
 * <p>
 * It runs under its own consumer group so that tracking never competes with
 * the projection consumer for partitions.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationFailureTracker {

    private final ObjectMapper objectMapper;
    private final NotificationFailureService failureService;

    @KafkaListener(topics = NotificationConstants.EVENTS_DLQ_TOPIC,
            groupId = NotificationConstants.DLQ_TRACKER_GROUP_ID,
            containerFactory = "notificationKafkaListenerContainerFactory")
    public void trackFailure(
            ConsumerRecord<String, String> record,
            @Header(name = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false) String errorMessage,
            @Header(name = KafkaHeaders.DLT_ORIGINAL_TOPIC, required = false) String originalTopic,
            @Header(name = KafkaHeaders.DLT_ORIGINAL_PARTITION, required = false) Integer originalPartition,
            @Header(name = KafkaHeaders.DLT_ORIGINAL_OFFSET, required = false) Long originalOffset) {

        String rawEvent = record != null ? record.value() : null;
        DomainEventDto event = null;
        if (rawEvent != null && !rawEvent.isBlank()) {
            try {
                event = objectMapper.readValue(rawEvent, DomainEventDto.class);
            } catch (Exception e) {
                log.debug("Dead letter record could not be parsed — it is recorded without an event id");
            }
        }

        String errorType = headerAsString(record, KafkaHeaders.DLT_EXCEPTION_FQCN);

        failureService.recordFailure(
                event,
                rawEvent,
                errorType,
                errorMessage,
                originalTopic != null ? originalTopic
                        : (record != null ? record.topic() : NotificationConstants.EVENTS_TOPIC),
                originalPartition != null ? originalPartition : (record != null ? record.partition() : null),
                originalOffset != null ? originalOffset : (record != null ? record.offset() : null));
    }

    /**
     * Read a header as a string. The exception headers hold byte arrays.
     */
    private String headerAsString(ConsumerRecord<String, String> record, String name) {
        if (record == null || record.headers() == null) {
            return null;
        }
        var header = record.headers().lastHeader(name);
        if (header == null || header.value() == null) {
            return null;
        }
        Object value = header.value();
        return value instanceof byte[] bytes
                ? new String(bytes, StandardCharsets.UTF_8)
                : value.toString();
    }
}
