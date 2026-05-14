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
     * Broadcast message to all participants in conversation
     * Sends to /topic/conversations/{conversationId} destination
     *
     * @param message Message to broadcast
     */
    public void broadcastToConversation(Message message) {
        if (message == null || message.getConversationId() == null) {
            log.warn("Cannot broadcast null message or message without conversationId");
            return;
        }

        try {
            String destination = "/topic/conversations/" + message.getConversationId();
            messagingTemplate.convertAndSend(destination, message);
            log.debug("Message broadcasted to {} - MessageID: {}", destination, message.getId());

        } catch (Exception e) {
            log.error("Error broadcasting message to conversation", e);
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

        try {
            String destination = "/user/" + userId + "/queue/messages";
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/messages",
                    message);
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
