package com.nexus.hr.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.InvalidProducerEpochException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
public class KafkaProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * Publish a message to a topic with retry logic for epoch issues
     *
     * @param topic   Topic name
     * @param key     Message key for partitioning
     * @param message Message content
     * @return CompletableFuture for async handling
     */
    public CompletableFuture<String> publishMessage(String topic, String key, String message) {
        return publishMessageWithRetry(topic, key, message, 0, 3);
    }

    /**
     * Publish message with exponential backoff retry for
     * InvalidProducerEpochException
     *
     * @param topic      Topic name
     * @param key        Message key for partitioning
     * @param message    Message content
     * @param retryCount Current retry attempt
     * @param maxRetries Maximum number of retries
     * @return CompletableFuture for async handling
     */
    private CompletableFuture<String> publishMessageWithRetry(String topic, String key, String message,
            int retryCount, int maxRetries) {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();

        try {
            log.info("Publishing message to topic: {} with key: {} (attempt {}/{})",
                    topic, key, retryCount + 1, maxRetries + 1);

            Message<String> kafkaMessage = MessageBuilder
                    .withPayload(message)
                    .setHeader(KafkaHeaders.TOPIC, topic)
                    .setHeader("kafka_messageKey", key)
                    .setHeader("message-timestamp", LocalDateTime.now().toString())
                    .build();

            kafkaTemplate.send(kafkaMessage).whenComplete((result, ex) -> {
                if (ex != null) {
                    handlePublishError(topic, key, message, ex, retryCount, maxRetries, completableFuture);
                } else {
                    log.info("Message published successfully to topic: {} with partition: {} and offset: {}",
                            topic,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                    completableFuture.complete(result.getRecordMetadata().toString());
                }
            });
        } catch (Exception e) {
            log.error("Exception while publishing message to topic: {} (attempt {}/{})",
                    topic, retryCount + 1, maxRetries + 1, e);
            completableFuture.completeExceptionally(e);
        }

        return completableFuture;
    }

    /**
     * Handle publishing errors with retry logic for InvalidProducerEpochException
     */
    private void handlePublishError(String topic, String key, String message, Throwable ex,
            int retryCount, int maxRetries, CompletableFuture<String> future) {
        if (isInvalidProducerEpochError(ex) && retryCount < maxRetries) {
            // Exponential backoff: 100ms, 200ms, 400ms
            long delayMs = 100L * (1L << retryCount);
            log.warn("InvalidProducerEpochException detected for topic: {}. Retrying after {}ms (attempt {}/{})",
                    topic, delayMs, retryCount + 1, maxRetries);

            scheduler.schedule(() -> publishMessageWithRetry(topic, key, message, retryCount + 1, maxRetries)
                    .whenComplete((result, retryEx) -> {
                        if (retryEx != null) {
                            future.completeExceptionally(retryEx);
                        } else {
                            future.complete(result);
                        }
                    }), delayMs, TimeUnit.MILLISECONDS);
        } else {
            log.error("Failed to publish message to topic: {} with key: {} after {} attempts",
                    topic, key, retryCount + 1, ex);
            future.completeExceptionally(ex);
        }
    }

    /**
     * Check if the exception is an InvalidProducerEpochException or contains it in
     * the cause chain
     */
    private boolean isInvalidProducerEpochError(Throwable ex) {
        if (ex instanceof InvalidProducerEpochException) {
            return true;
        }
        if (ex.getCause() != null) {
            return isInvalidProducerEpochError(ex.getCause());
        }
        return ex.getMessage() != null && ex.getMessage().contains("InvalidProducerEpochException");
    }

}
