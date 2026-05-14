package com.nexus.cms.controller;

import com.nexus.cms.model.entities.Message;
import com.nexus.cms.payload.ChatPayload;
import com.nexus.cms.service.ChatService;
import com.nexus.cms.service.MessageBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket Controller for Real-Time Chat
 *
 * STOMP Endpoints:
 * - /app/chat/send - Send message to conversation
 * - /topic/conversations/{conversationId} - Subscribe to conversation messages
 * - /user/{userId}/queue/messages - Private message queue
 * - /topic/conversations/{conversationId}/presence - Presence events
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final ChatService chatService;
    private final MessageBroadcaster messageBroadcaster;

    /**
     * Handle incoming message from WebSocket client
     * Client sends: /app/chat/send with message payload
     * Server broadcasts: /topic/conversations/{conversationId}
     *
     * @param request        ChatPayload.SendMessageRequest with conversationId,
     *                       content
     * @param principal      Authenticated user (from JWT token)
     * @param headerAccessor Session headers
     * @return Broadcasted message
     */
    @MessageMapping("/chat/send")
    @SendTo("/topic/conversations/{conversationId}")
    public Message handleMessage(
            ChatPayload.SendMessageRequest request,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor) {

        try {
            Long senderId = Long.valueOf(principal.getName());

            log.debug("WebSocket message received - Sender: {}, Conversation: {}",
                    senderId, request.getConversationId());

            // Send message (will publish to Kafka)
            Message message = chatService.sendMessage(
                    request.getConversationId(),
                    senderId,
                    request.getContent(),
                    request.getOrgId());

            log.info("Message broadcast - ID: {}, Conversation: {}",
                    message.getId(), message.getConversationId());

            return message;

        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
            // Return error message to sender
            messageBroadcaster.sendNotification(
                    Long.valueOf(principal.getName()),
                    "Error",
                    "Failed to send message: " + e.getMessage());
            return null;
        }
    }

    /**
     * Handle typing indicator event
     * Client sends: /app/chat/typing with conversationId, userId
     * Server broadcasts: /topic/conversations/{conversationId}/typing
     */
    @MessageMapping("/chat/typing")
    @SendTo("/topic/conversations/{conversationId}/typing")
    public MessageBroadcaster.PresenceEvent handleTyping(
            ChatPayload.TypingIndicatorRequest request,
            Principal principal) {

        try {
            Long userId = Long.valueOf(principal.getName());

            log.debug("Typing indicator - User: {}, Conversation: {}",
                    userId, request.getConversationId());

            return MessageBroadcaster.PresenceEvent.builder()
                    .conversationId(request.getConversationId())
                    .userId(userId)
                    .eventType("TYPING")
                    .timestamp(System.currentTimeMillis())
                    .build();

        } catch (Exception e) {
            log.error("Error handling typing indicator", e);
            return null;
        }
    }

    /**
     * Handle stop typing event
     * Client sends: /app/chat/typing/stop with conversationId, userId
     * Server broadcasts: /topic/conversations/{conversationId}/typing
     */
    @MessageMapping("/chat/typing/stop")
    @SendTo("/topic/conversations/{conversationId}/typing")
    public MessageBroadcaster.PresenceEvent handleStopTyping(
            ChatPayload.TypingIndicatorRequest request,
            Principal principal) {

        try {
            Long userId = Long.valueOf(principal.getName());

            log.debug("Stop typing indicator - User: {}, Conversation: {}",
                    userId, request.getConversationId());

            return MessageBroadcaster.PresenceEvent.builder()
                    .conversationId(request.getConversationId())
                    .userId(userId)
                    .eventType("STOP_TYPING")
                    .timestamp(System.currentTimeMillis())
                    .build();

        } catch (Exception e) {
            log.error("Error handling stop typing event", e);
            return null;
        }
    }

    /**
     * Handle user joining conversation
     * Broadcasts presence event to all participants
     */
    @MessageMapping("/chat/joined/{conversationId}")
    public void handleUserJoined(
            @DestinationVariable Long conversationId,
            Principal principal) {

        try {
            Long userId = Long.valueOf(principal.getName());

            log.info("User joined conversation - User: {}, Conversation: {}",
                    userId, conversationId);

            messageBroadcaster.broadcastPresenceEvent(
                    conversationId,
                    userId,
                    "JOINED");

        } catch (Exception e) {
            log.error("Error handling user joined event", e);
        }
    }

    /**
     * Handle user leaving conversation
     * Broadcasts presence event to remaining participants
     */
    @MessageMapping("/chat/left/{conversationId}")
    public void handleUserLeft(
            @DestinationVariable Long conversationId,
            Principal principal) {

        try {
            Long userId = Long.valueOf(principal.getName());

            log.info("User left conversation - User: {}, Conversation: {}",
                    userId, conversationId);

            messageBroadcaster.broadcastPresenceEvent(
                    conversationId,
                    userId,
                    "LEFT");

        } catch (Exception e) {
            log.error("Error handling user left event", e);
        }
    }

    /**
     * Health check / ping endpoint
     * Allows clients to test connection
     */
    @MessageMapping("/chat/ping")
    public ChatPayload.PingResponse handlePing() {
        return ChatPayload.PingResponse.builder()
                .timestamp(System.currentTimeMillis())
                .status("PONG")
                .build();
    }
}
