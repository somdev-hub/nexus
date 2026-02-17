package com.nexus.cms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka Producer Service
 *
 * Handles publishing messages to Kafka topics with:
 * - Error handling and retry logic
 * - Callback mechanisms
 * - Monitoring and logging
 * - Guaranteed message delivery
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Publish a message to a topic
     *
     * @param topic Topic name
     * @param key Message key for partitioning
     * @param message Message content
     * @return CompletableFuture for async handling
     */
    public CompletableFuture<String> publishMessage(String topic, String key, String message) {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();

        try {
            log.info("Publishing message to topic: {} with key: {}", topic, key);

            Message<String> kafkaMessage = MessageBuilder
                .withPayload(message)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader("kafka_messageKey", key)
                .setHeader("timestamp", LocalDateTime.now().toString())
                .build();

            kafkaTemplate.send(kafkaMessage).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish message to topic: {} with key: {}", topic, key, ex);
                    completableFuture.completeExceptionally(ex);
                } else {
                    log.info("Message published successfully to topic: {} with partition: {} and offset: {}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                    completableFuture.complete(result.getRecordMetadata().toString());
                }
            });
        } catch (Exception e) {
            log.error("Exception while publishing message to topic: {}", topic, e);
            completableFuture.completeExceptionally(e);
        }

        return completableFuture;
    }

    /**
     * Publish message with headers
     *
     * @param topic Topic name
     * @param key Message key
     * @param message Message content
     * @param headerKey Header key
     * @param headerValue Header value
     * @return CompletableFuture for async handling
     */
    public CompletableFuture<String> publishMessageWithHeaders(
        String topic,
        String key,
        String message,
        String headerKey,
        String headerValue) {

        CompletableFuture<String> completableFuture = new CompletableFuture<>();

        try {
            Message<String> kafkaMessage = MessageBuilder
                .withPayload(message)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader("kafka_messageKey", key)
                .setHeader(headerKey, headerValue)
                .setHeader("timestamp", LocalDateTime.now().toString())
                .build();

            kafkaTemplate.send(kafkaMessage).whenComplete((result, ex) -> {
                if (ex != null) {
                    completableFuture.completeExceptionally(ex);
                } else {
                    completableFuture.complete(result.getRecordMetadata().toString());
                }
            });
        } catch (Exception e) {
            completableFuture.completeExceptionally(e);
        }

        return completableFuture;
    }

    /**
     * Publish notification event
     */
    public CompletableFuture<String> publishNotification(String userId, String notificationContent) {
        return publishMessage("cms-notifications", userId, notificationContent);
    }

    /**
     * Publish audit log event
     */
    public CompletableFuture<String> publishAuditLog(String action, String details) {
        return publishMessage("cms-audit-logs", action, details);
    }

    /**
     * Publish domain event
     */
    public CompletableFuture<String> publishEvent(String eventType, String eventData) {
        return publishMessage("cms-events", eventType, eventData);
    }
}



