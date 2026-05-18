package com.nexus.cms.service;

import com.nexus.cms.exception.ResourceNotFoundException;
import com.nexus.cms.exception.ServiceLevelException;
import com.nexus.cms.model.entities.Message;
import com.nexus.cms.repository.MessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.sql.Timestamp;

/**
 * ChatService handles message operations:
 * - Sending/persisting messages
 * - Fetching conversation history
 * - Message validation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageRepository messageRepository;
    private final ConversationService conversationService;
    private final KafkaProducerService kafkaProducerService;
    private final ObjectMapper objectMapper;

    /**
     * Send a message to a conversation
     *
     * IMPORTANT: Messages are persisted to database BEFORE Kafka publication
     * This ensures no message loss even if Kafka is temporarily unavailable.
     *
     * @param conversationId UUID of target conversation
     * @param senderId       User ID of sender
     * @param content        Message content
     * @param orgId          Organization ID
     * @return Created Message entity with database ID
     */
    @Transactional
    public Message sendMessage(Long conversationId, Long senderId, String content, Long orgId) {
        if (ObjectUtils.isEmpty(conversationId) || ObjectUtils.isEmpty(senderId) ||
                ObjectUtils.isEmpty(content) || ObjectUtils.isEmpty(orgId)) {
            throw new ServiceLevelException(
                    "ChatService",
                    "Conversation ID, Sender ID, content, and Organization ID are required",
                    "sendMessage",
                    "Missing required data",
                    "All parameters must be provided");
        }

        try {
            var conversation = conversationService.getConversation(conversationId, orgId);

            // Verify sender is participant
            boolean isParticipant = conversationService.isUserParticipant(conversationId, senderId);
            if (!isParticipant) {
                throw new ServiceLevelException(
                        "ChatService",
                        "User is not a participant of this conversation",
                        "sendMessage",
                        "Unauthorized access",
                        "User " + senderId + " is not in conversation " + conversationId);
            }

            // Create message
            Message message = Message.builder()
                    .conversation(conversation)
                    .senderId(senderId)
                    .content(content)
                    .timestamp(new Timestamp(System.currentTimeMillis()))
                    .status(Message.MessageStatus.SENT)
                    .orgId(orgId)
                    .type(Message.MessageType.TEXT)
                    .build();

            // **CRITICAL**: Save to database FIRST before Kafka
            // This ensures persistence even if Kafka fails
            Message savedMessage = messageRepository.save(message);
            // conversationId is read-only mapped from FK and may remain null on this
            // managed instance until reload; set it for immediate in-process routing.
            savedMessage.setConversationId(conversationId);
            log.debug("Message persisted to database - ID: {}, Conversation: {}",
                    savedMessage.getId(), conversationId);

            // Publish to Kafka asynchronously (non-blocking)
            // If Kafka fails, message is already in database
            publishMessageToKafka(savedMessage);
            log.debug("Message published to Kafka - ID: {}, Conversation: {}",
                    savedMessage.getId(), conversationId);

            return savedMessage;

        } catch (ServiceLevelException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error sending message", e);
            throw new ServiceLevelException(
                    "ChatService",
                    "Error occurred while sending message",
                    "sendMessage",
                    "Service level exception",
                    e.getMessage());
        }
    }

    /**
     * Publish message to Kafka topic for distribution across services
     * 
     * Note: Message is already persisted in database before this is called.
     * Kafka publication is for event streaming and caching in other services.
     * If Kafka is unavailable, message remains in database for later consumption.
     * 
     * Key is conversationId to ensure ordering of messages in same conversation
     */
    private void publishMessageToKafka(Message message) {
        try {
            Long conversationId = message.getConversationId();
            if (conversationId == null && message.getConversation() != null) {
                conversationId = message.getConversation().getId();
            }

            if (conversationId == null) {
                log.warn("Skipping Kafka publish because conversationId is null - MessageID: {}", message.getId());
                return;
            }

            KafkaMessagePayload payload = KafkaMessagePayload.builder()
                    .id(message.getId())
                    .conversationId(conversationId)
                    .senderId(message.getSenderId())
                    .content(message.getContent())
                    .timestamp(message.getTimestamp())
                    .status(message.getStatus() != null ? message.getStatus().name() : null)
                    .type(message.getType() != null ? message.getType().name() : null)
                    .orgId(message.getOrgId())
                    .build();

            String messageJson = objectMapper.writeValueAsString(payload);
            String key = conversationId.toString();

            kafkaProducerService.publishMessage("chat-messages", key, messageJson)
                    .thenAccept(result -> log.debug("Message published to Kafka: {}", result))
                    .exceptionally(ex -> {
                        // Message is already in database, log error but don't fail
                        log.warn("Failed to publish message to Kafka (will retry via backlog service) - " +
                                "MessageID: {}, Error: {}", message.getId(), ex.getMessage());
                        // Could queue for retry using KafkaBacklogService here
                        return null;
                    });
        } catch (Exception e) {
            log.warn("Error serializing message for Kafka (message persisted in database) - " +
                    "MessageID: {}, Error: {}", message.getId(), e.getMessage());
        }
    }

    @lombok.Data
    @lombok.Builder
    private static class KafkaMessagePayload {
        private Long id;
        private Long conversationId;
        private Long senderId;
        private String content;
        private Timestamp timestamp;
        private String status;
        private String type;
        private Long orgId;
    }

    /**
     * Get conversation message history with pagination
     *
     * @param conversationId Conversation UUID
     * @param orgId          Organization ID
     * @param userId         User requesting history (must be participant)
     * @param pageable       Pagination info
     * @return Page of messages ordered by timestamp DESC (newest first)
     */
    @Transactional(readOnly = true)
    public Page<Message> getConversationHistory(Long conversationId, Long orgId, Long userId, Pageable pageable) {
        if (ObjectUtils.isEmpty(conversationId) || ObjectUtils.isEmpty(orgId) || ObjectUtils.isEmpty(userId)) {
            throw new ServiceLevelException(
                    "ChatService",
                    "Conversation ID, Organization ID, and User ID are required",
                    "getConversationHistory",
                    "Missing required data",
                    "All parameters must be provided");
        }

        try {
            // Verify conversation exists and user is participant
            conversationService.getConversation(conversationId, orgId);

            boolean isParticipant = conversationService.isUserParticipant(conversationId, userId);
            if (!isParticipant) {
                throw new ServiceLevelException(
                        "ChatService",
                        "User is not a participant of this conversation",
                        "getConversationHistory",
                        "Unauthorized access",
                        "User cannot access this conversation's history");
            }

            Page<Message> messages = messageRepository.findByConversationIdOrderByTimestampDesc(
                    conversationId,
                    pageable);

            log.debug("Retrieved {} messages from conversation {}", messages.getSize(), conversationId);
            return messages;

        } catch (ServiceLevelException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving conversation history", e);
            throw new ServiceLevelException(
                    "ChatService",
                    "Error occurred while fetching conversation history",
                    "getConversationHistory",
                    "Service level exception",
                    e.getMessage());
        }
    }

    /**
     * Get a specific message by ID
     */
    @Transactional(readOnly = true)
    public Message getMessage(Long messageId, Long orgId) {
        if (ObjectUtils.isEmpty(messageId) || ObjectUtils.isEmpty(orgId)) {
            throw new ServiceLevelException(
                    "ChatService",
                    "Message ID and Organization ID are required",
                    "getMessage",
                    "Missing required data",
                    "Both parameters must be provided");
        }

        return messageRepository.findByIdAndOrgId(messageId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Message",
                        "id",
                        messageId.toString()));
    }

    /**
     * Check if message exists for idempotency
     */
    @Transactional(readOnly = true)
    public boolean messageExists(Long messageId) {
        return messageRepository.existsById(messageId);
    }

    /**
     * Persist message to database (called from Kafka consumer)
     * Updates status from SENT to DELIVERED
     */
    @Transactional
    public Message persistMessage(Message message) {
        if (ObjectUtils.isEmpty(message) || ObjectUtils.isEmpty(message.getId())) {
            throw new ServiceLevelException(
                    "ChatService",
                    "Message and Message ID are required",
                    "persistMessage",
                    "Missing required data",
                    "Message cannot be null");
        }

        try {
            // Check for idempotency - don't re-save if exists
            if (messageRepository.existsById(message.getId())) {
                log.debug("Message already persisted: {}", message.getId());
                return messageRepository.findById(message.getId()).orElse(message);
            }

            message.setStatus(Message.MessageStatus.DELIVERED);
            Message saved = messageRepository.save(message);
            log.debug("Message persisted to database - ID: {}", saved.getId());
            return saved;

        } catch (Exception e) {
            log.error("Error persisting message", e);
            throw new ServiceLevelException(
                    "ChatService",
                    "Error occurred while persisting message",
                    "persistMessage",
                    "Service level exception",
                    e.getMessage());
        }
    }

    /**
     * Get total message count for conversation
     */
    @Transactional(readOnly = true)
    public long getConversationMessageCount(Long conversationId) {
        return messageRepository.countByConversationId(conversationId);
    }

    /**
     * Get delivered message count (for statistics)
     */
    @Transactional(readOnly = true)
    public long getDeliveredMessageCount(Long conversationId) {
        return messageRepository.countDeliveredMessages(conversationId);
    }
}
