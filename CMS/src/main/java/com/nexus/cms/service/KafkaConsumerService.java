package com.nexus.cms.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Kafka Consumer Service
 *
 * Handles consuming messages from Kafka topics with:
 * - Batch processing for efficiency
 * - Manual acknowledgment for reliability
 * - Error handling and retry logic
 * - Monitoring and logging
 */
@Slf4j
@Service
public class KafkaConsumerService {

    /**
     * Consume notification messages (batch)
     * Processes multiple messages in one go for efficiency
     */
    @KafkaListener(
        topics = "cms-notifications",
        groupId = "cms-notification-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeNotifications(
        List<String> messages,
        @Header(name = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition,
        @Header(name = KafkaHeaders.OFFSET, required = false) Long offset,
        Acknowledgment acknowledgment) {

        try {
            log.info("Received batch of {} notifications from partition: {}, offset: {}",
                messages.size(), partition, offset);

            for (String message : messages) {
                processNotification(message);
            }

            // Manually commit offset after successful processing
            acknowledgment.acknowledge();
            log.info("Notifications batch processed and committed successfully");

        } catch (Exception e) {
            log.error("Error processing notifications batch from partition: {}", partition, e);
            // Don't acknowledge on error - message will be reprocessed
        }
    }

    /**
     * Consume audit logs (batch)
     */
    @KafkaListener(
        topics = "cms-audit-logs",
        groupId = "cms-audit-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAuditLogs(
        List<String> messages,
        @Header(name = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition,
        @Header(name = KafkaHeaders.OFFSET, required = false) Long offset,
        Acknowledgment acknowledgment) {

        try {
            log.info("Received batch of {} audit logs from partition: {}", messages.size(), partition);

            for (String message : messages) {
                processAuditLog(message);
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing audit logs batch", e);
        }
    }

    /**
     * Consume events (batch)
     */
    @KafkaListener(
        topics = "cms-events",
        groupId = "cms-event-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeEvents(
        List<String> messages,
        @Header(name = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition,
        @Header(name = KafkaHeaders.OFFSET, required = false) Long offset,
        Acknowledgment acknowledgment) {

        try {
            log.info("Received batch of {} events from partition: {}", messages.size(), partition);

            for (String message : messages) {
                processEvent(message);
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing events batch", e);
        }
    }

    /**
     * Alternative: Single record consumer (uncomment if needed)
     * Use this instead of batch for high-latency requirements
     */
    /*
    @KafkaListener(
        topics = "cms-notifications",
        groupId = "cms-notification-group-single",
        containerFactory = "singleRecordKafkaListenerContainerFactory"
    )
    public void consumeNotificationSingleRecord(
        @Payload String message,
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment acknowledgment) {

        try {
            log.info("Processing single notification: key={}, partition={}, offset={}",
                key, partition, offset);

            processNotification(message);
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing single notification", e);
        }
    }
    */

    /**
     * Process notification message
     */
    private void processNotification(String message) {
        try {
            log.debug("Processing notification: {}", message);
            // Add your business logic here
            // Example: Send email, push notification, etc.
        } catch (Exception e) {
            log.error("Error processing notification: {}", message, e);
            throw e;
        }
    }

    /**
     * Process audit log message
     */
    private void processAuditLog(String message) {
        try {
            log.debug("Processing audit log: {}", message);
            // Add your business logic here
            // Example: Store in database, write to file, etc.
        } catch (Exception e) {
            log.error("Error processing audit log: {}", message, e);
            throw e;
        }
    }

    /**
     * Process event message
     */
    private void processEvent(String message) {
        try {
            log.debug("Processing event: {}", message);
            // Add your business logic here
            // Example: Update cache, trigger workflows, etc.
        } catch (Exception e) {
            log.error("Error processing event: {}", message, e);
            throw e;
        }
    }
}







