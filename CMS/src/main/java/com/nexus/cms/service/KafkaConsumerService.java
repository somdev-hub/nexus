package com.nexus.cms.service;

import com.nexus.cms.model.enums.CommsType;
import com.nexus.cms.util.WebConstants;
import com.nexus.cms.model.entities.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonParseException;

import java.util.List;
import java.util.Map;

/**
 * Kafka Consumer Service
 * <p>
 * Handles consuming messages from Kafka topics with:
 * - Batch processing for efficiency
 * - Manual acknowledgment for reliability
 * - Error handling and retry logic
 * - Monitoring and logging
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final WebConstants webConstants;
    private final ObjectMapper objectMapper;
    private final EmailCommunicationService emailCommunicationService;
    private final KafkaBacklogService kafkaBacklogService;
    private final ChatService chatService;                    // NEW: for chat message handling
    private final MessageBroadcaster messageBroadcaster;      // NEW: for WebSocket delivery

    /**
     * Consume chat messages from chat-messages topic
     * Persists messages to database and broadcasts via WebSocket
     */
    @KafkaListener(topics = "chat-messages", groupId = "cms-chat-group", containerFactory = "singleRecordKafkaListenerContainerFactory")
    public void consumeChatMessage(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        try {
            String trimmedMessage = message.trim();

            if (trimmedMessage.isEmpty()) {
                log.warn("Received empty message from chat-messages topic");
                acknowledgment.acknowledge();
                return;
            }

            log.debug("Processing chat message from partition: {} offset: {}", partition, offset);

            // Deserialize message
            Message chatMessage = objectMapper.readValue(trimmedMessage, Message.class);

            // Persist to database (idempotency check inside service)
            Message persistedMessage = chatService.persistMessage(chatMessage);

            // Broadcast via WebSocket to all conversation participants
            messageBroadcaster.broadcastToConversation(persistedMessage);

            log.info("Chat message processed and broadcasted - ID: {}, Conversation: {}",
                    chatMessage.getId(), chatMessage.getConversationId());

            // Acknowledge after successful processing
            acknowledgment.acknowledge();

        } catch (JsonParseException e) {
            log.error("JSON parsing error for chat message. Key: {}, offset: {}. Error: {}",
                    key, offset, e.getMessage(), e);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing chat message from partition: {}, offset: {}", partition, offset, e);
            // Don't acknowledge on error - message will be reprocessed
        }
    }

    /**
     * Consume notification messages (batch)
     * Processes multiple messages in one go for efficiency
     */
    @KafkaListener(topics = "cms-notifications", groupId = "cms-notification-group", containerFactory = "kafkaListenerContainerFactory")
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
    @KafkaListener(topics = "cms-audit-logs", groupId = "cms-audit-group", containerFactory = "kafkaListenerContainerFactory")
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
    @KafkaListener(topics = "cms-events", groupId = "cms-event-group", containerFactory = "kafkaListenerContainerFactory")
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
     * @deprecated This method is deprecated and will be removed in future versions. Use the new hr-kafka-mail-topic listener instead.
     */
    @Deprecated
    @KafkaListener(topics = "candidate-selection-mail-topic", groupId = "cms-group", containerFactory = "singleRecordKafkaListenerContainerFactory")
    public void consumeCandidateSelectionMail(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        try {
            // Trim whitespace and validate message
            String trimmedMessage = message.trim();

            if (trimmedMessage.isEmpty()) {
                log.warn("Received empty message from topic candidate-selection-mail");
                acknowledgment.acknowledge();
                return;
            }

            log.debug("Raw Kafka message (first 500 chars): {}",
                    trimmedMessage.length() > 500 ? trimmedMessage.substring(0, 500) : trimmedMessage);

            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(trimmedMessage, Map.class);

            if (payload.containsKey("commsType") && payload.containsKey("uuid") && payload.containsKey("topic")
                    && payload.get("commsType").equals("email") && payload.containsKey("orgId")) {
                log.info("Processing candidate selection email with key: {}, offset: {}", key, offset);
                kafkaBacklogService.logReceived(
                        payload.get("topic").toString(),
                        payload.get("uuid").toString(),
                        Long.valueOf(payload.get("orgId").toString()),
                        payload.getOrDefault("templateParam", "").toString());
                emailCommunicationService.handleEmailCommunication(trimmedMessage);
                acknowledgment.acknowledge();
            } else {
                log.warn("Received message with unsupported commsType: {}", payload.get("commsType"));
                acknowledgment.acknowledge();
            }
        } catch (JsonParseException e) {
            log.error(
                    "JSON parsing error for candidate selection email. Message content (first 1000 chars): {}. Error: {}",
                    message.length() > 1000 ? message.substring(0, 1000) : message,
                    e.getMessage(), e);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing candidate selection email with key: {}, offset: {}", key, offset, e);
            acknowledgment.acknowledge();
        }
    }

    /**
     * Consume salary payment email messages from PMS
     * Triggers email notifications when salary payments are successfully processed
     * @deprecated This method is deprecated and will be removed in future versions. Use the new hr-kafka-mail-topic listener instead.
     */
    @Deprecated
    @KafkaListener(topics = "salary-payment-mail-topic", groupId = "cms-group", containerFactory = "singleRecordKafkaListenerContainerFactory")
    public void consumeSalaryPaymentMail(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        try {
            String trimmedMessage = message.trim();

            if (trimmedMessage.isEmpty()) {
                log.warn("Received empty message from topic salary-payment-mail");
                acknowledgment.acknowledge();
                return;
            }

            log.debug("Raw Kafka message for salary payment (first 500 chars): {}",
                    trimmedMessage.length() > 500 ? trimmedMessage.substring(0, 500) : trimmedMessage);

            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(trimmedMessage, Map.class);

            if (payload.containsKey("commsType") && payload.containsKey("uuid") && payload.containsKey("topic")
                    && payload.get("commsType").equals("email") && payload.containsKey("orgId")) {
                log.info("Processing salary payment email with key: {}, offset: {}", key, offset);
                String topic = payload.get("topic").toString();
                String uuid = payload.get("uuid").toString();
                Long orgId = Long.valueOf(payload.get("orgId").toString());
                String templateParam = payload.getOrDefault("templateParam", "").toString();

                // Log backlog in separate transaction to avoid aborting main transaction if it fails
                try {
                    kafkaBacklogService.logReceived(topic, uuid, orgId, templateParam);
                } catch (Exception e) {
                    log.error(
                            "Failed to log kafka backlog for topic: {} and uuid: {}. Continuing with email processing.",
                            topic, uuid, e);
                    // Continue - don't let backlog failure prevent email processing
                }

                // Process email in try-catch to handle attachment failures gracefully
                try {
                    emailCommunicationService.handleEmailCommunication(trimmedMessage);
                } catch (Exception e) {
                    log.error("Failed to handle email communication for uuid: {}. Error: {}", uuid, e.getMessage(), e);
                    // Log the error but acknowledge the message to avoid messages stuck in Kafka
                }

                acknowledgment.acknowledge();
            } else {
                log.warn("Received message with unsupported commsType: {}", payload.get("commsType"));
                acknowledgment.acknowledge();
            }
        } catch (JsonParseException e) {
            log.error(
                    "JSON parsing error for salary payment email. Message content (first 1000 chars): {}. Error: {}",
                    message.length() > 1000 ? message.substring(0, 1000) : message,
                    e.getMessage(), e);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing salary payment email with key: {}, offset: {}", key, offset, e);
            acknowledgment.acknowledge();
        }
    }

    @KafkaListener(topics = "hr-kafka-mail-topic", groupId = "cms-group", containerFactory = "singleRecordKafkaListenerContainerFactory")
    public void consumeHrKafkaMail(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        try {
            String trimmedMessage = message.trim();

            if (trimmedMessage.isEmpty()) {
                log.warn("Received empty message from topic salary-payment-mail");
                acknowledgment.acknowledge();
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(trimmedMessage, Map.class);

            if (payload.containsKey("commsType") && payload.containsKey("uuid")
                    && payload.get("commsType").equals(CommsType.EMAIL.name())) {
                log.info("Processing salary payment email with key: {}, offset: {}", key, offset);
                String uuid = payload.get("uuid").toString();
                try {
                    emailCommunicationService.handleEmailCommunication(trimmedMessage);
                } catch (Exception e) {
                    log.error("Failed to handle email communication for uuid: {}. Error: {}", uuid, e.getMessage(), e);
                    // Log the error but acknowledge the message to avoid messages stuck in Kafka
                }

                acknowledgment.acknowledge();
            } else {
                log.warn("Received message with unsupported commsType: {}", payload.get("commsType"));
                acknowledgment.acknowledge();
            }
        } catch (JsonParseException e) {
            log.error(
                    "JSON parsing error for salary payment email. Message content (first 1000 chars): {}. Error: {}",
                    message.length() > 1000 ? message.substring(0, 1000) : message,
                    e.getMessage(), e);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing salary payment email with key: {}, offset: {}", key, offset, e);
            acknowledgment.acknowledge();
        }
    }


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
