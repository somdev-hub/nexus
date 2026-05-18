package com.nexus.cms.service;

import com.nexus.cms.model.entities.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * MessageBroadcaster handles real-time message distribution via WebSocket
 * Routes messages to appropriate STOMP destinations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast message to all participants EXCEPT the sender
     * This prevents the sender from receiving their own message twice
     * Sender gets a separate confirmation via sendToUser()
     * 
     * Sends to /topic/conversations/{conversationId} destination
     *
     * @param message Message to broadcast
     */
    public void broadcastToConversation(Message message) {
        if (message == null) {
            log.warn("Cannot broadcast null message");
            return;
        }

        Long conversationId = resolveConversationId(message);
        if (conversationId == null) {
            log.warn("Cannot broadcast message without conversationId - MessageID: {}", message.getId());
            return;
        }

        try {
            String destination = "/topic/conversations/" + conversationId;
            WebSocketMessagePayload payload = toWebSocketMessage(message, conversationId);

            // Add a field to indicate this is for other users (not the sender)
            // The frontend can use this to avoid showing duplicate
            payload.setExcludeUserId(message.getSenderId());

            messagingTemplate.convertAndSend(destination, payload);
            log.debug("Message broadcasted to {} (excluding sender {}) - MessageID: {}",
                    destination, message.getSenderId(), message.getId());

        } catch (Exception e) {
            log.error("Error broadcasting message to conversation", e);
        }
    }

    /**
     * Send message confirmation to sender only
     * Sender receives this to display their message locally after confirmation
     * 
     * Sends to /user/{userId}/queue/message-confirmation
     *
     * @param message Message to confirm
     */
    public void sendMessageConfirmationToSender(Message message) {
        if (message == null) {
            log.warn("Cannot send confirmation for null message");
            return;
        }

        Long senderId = message.getSenderId();
        if (senderId == null) {
            log.warn("Cannot send confirmation without senderId - MessageID: {}", message.getId());
            return;
        }

        Long conversationId = resolveConversationId(message);
        if (conversationId == null) {
            log.warn("Cannot send confirmation without conversationId - MessageID: {}", message.getId());
            return;
        }

        try {
            WebSocketMessagePayload payload = toWebSocketMessage(message, conversationId);
            payload.setConfirmed(true);

            messagingTemplate.convertAndSendToUser(
                    senderId.toString(),
                    "/queue/message-confirmation",
                    payload);
            log.debug("Message confirmation sent to sender {} - MessageID: {}", senderId, message.getId());

        } catch (Exception e) {
            log.error("Error sending message confirmation to sender {}", senderId, e);
        }
    }

    /**
     * Send message to specific user
     * Sends to /user/{userId}/queue/messages destination
     * Used for notifications, errors, or private messages
     *
     * @param message Message to send
     * @param userId  Target user ID
     */
    public void sendToUser(Message message, Long userId) {
        if (message == null || userId == null) {
            log.warn("Cannot send null message or message to null user");
            return;
        }

        Long conversationId = resolveConversationId(message);

        try {
            String destination = "/user/" + userId + "/queue/messages";
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/messages",
                    toWebSocketMessage(message, conversationId));
            log.debug("Message sent to user {} - MessageID: {}", userId, message.getId());

        } catch (Exception e) {
            log.error("Error sending message to user {}", userId, e);
        }
    }

    /**
     * Send notification to specific user
     * Lightweight operation for generic notifications
     *
     * @param userId  Target user ID
     * @param title   Notification title
     * @param message Notification message
     */
    public void sendNotification(Long userId, String title, String message) {
        if (userId == null || title == null || message == null) {
            log.warn("Cannot send notification with null parameters");
            return;
        }

        try {
            NotificationPayload notification = NotificationPayload.builder()
                    .title(title)
                    .message(message)
                    .timestamp(System.currentTimeMillis())
                    .build();

            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    notification);
            log.debug("Notification sent to user {} - Title: {}", userId, title);

        } catch (Exception e) {
            log.error("Error sending notification to user {}", userId, e);
        }
    }

    /**
     * Broadcast presence information (user joined, left, etc.)
     * System message for conversation participants
     *
     * @param conversationId Conversation ID
     * @param userId         User ID of presence event
     * @param eventType      "JOINED", "LEFT", "TYPING", etc.
     */
    public void broadcastPresenceEvent(Long conversationId, Long userId, String eventType) {
        if (conversationId == null || userId == null || eventType == null) {
            log.warn("Cannot broadcast presence event with null parameters");
            return;
        }

        try {
            PresenceEvent event = PresenceEvent.builder()
                    .conversationId(conversationId)
                    .userId(userId)
                    .eventType(eventType)
                    .timestamp(System.currentTimeMillis())
                    .build();

            String destination = "/topic/conversations/" + conversationId + "/presence";
            messagingTemplate.convertAndSend(destination, event);
            log.debug("Presence event broadcasted to {} - Event: {}, User: {}",
                    destination, eventType, userId);

        } catch (Exception e) {
            log.error("Error broadcasting presence event", e);
        }
    }

    private Long resolveConversationId(Message message) {
        Long conversationId = message.getConversationId();
        if (conversationId == null && message.getConversation() != null) {
            conversationId = message.getConversation().getId();
        }
        return conversationId;
    }

    private WebSocketMessagePayload toWebSocketMessage(Message message, Long conversationId) {
        return WebSocketMessagePayload.builder()
                .id(message.getId())
                .conversationId(conversationId)
                .senderId(message.getSenderId())
                .senderName("User " + message.getSenderId())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .status(message.getStatus() != null ? message.getStatus().name().toLowerCase() : null)
                .type(message.getType() != null ? message.getType().name() : null)
                .orgId(message.getOrgId())
                .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class WebSocketMessagePayload {
        private Long id;
        private Long conversationId;
        private Long senderId;
        private String senderName;
        private String content;
        private java.sql.Timestamp timestamp;
        private String status;
        private String type;
        private Long orgId;

        /**
         * When set, indicates this message should NOT be displayed to this user ID
         * Used when broadcasting: sender is excluded since they get a separate
         * confirmation
         */
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        private Long excludeUserId;

        /**
         * When true, indicates this is a confirmed message (already persisted to DB)
         * Used in sender confirmation: sender knows the message was successfully saved
         */
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        private Boolean confirmed;
    }

    /**
     * Inner class for notification payload
     */
    @lombok.Data
    @lombok.Builder
    public static class NotificationPayload {
        private String title;
        private String message;
        private Long timestamp;
    }

    /**
     * Inner class for presence event
     */
    @lombok.Data
    @lombok.Builder
    public static class PresenceEvent {
        private Long conversationId;
        private Long userId;
        private String eventType; // JOINED, LEFT, TYPING, etc.
        private Long timestamp;
    }
}
