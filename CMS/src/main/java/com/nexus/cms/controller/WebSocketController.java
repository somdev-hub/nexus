package com.nexus.cms.controller;

import com.nexus.cms.model.entities.Message;
import com.nexus.cms.payload.ChatPayload;
import com.nexus.cms.service.ChatService;
import com.nexus.cms.service.MessageBroadcaster;
import com.nexus.cms.service.SessionTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket Controller for Real-Time Chat
 *
 * IMPORTANT: This controller uses SimpMessagingTemplate for dynamic routing
 * instead of @SendTo annotations. This ensures proper message routing to
 * dynamically determined destinations based on request payload.
 *
 * STOMP Endpoints:
 * - /app/chat/send - Send message to conversation
 * - /topic/conversations/{conversationId} - Subscribe to conversation messages
 * - /user/{userId}/queue/messages - Private message queue
 * - /topic/conversations/{conversationId}/presence - Presence events
 * - /topic/conversations/{conversationId}/typing - Typing indicators
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final ChatService chatService;
    private final MessageBroadcaster messageBroadcaster;
    private final SessionTracker sessionTracker;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handle incoming message from WebSocket client
     * Client sends: /app/chat/send with message payload
     * Server broadcasts: /topic/conversations/{conversationId}
     *
     * @param request   ChatPayload.SendMessageRequest with conversationId, content,
     *                  userId, orgId
     * @param principal Authenticated user (from JWT token, may be null for
     *                  WebSocket)
     * @return Broadcasted message
     */
    @MessageMapping("/chat/send")
    public void handleMessage(
            ChatPayload.SendMessageRequest request,
            Principal principal) {

        try {
            // Get userId from request (preferred) or Principal (fallback)
            Long senderId = null;
            if (request.getUserId() != null) {
                senderId = request.getUserId();
            } else if (principal != null) {
                senderId = Long.valueOf(principal.getName());
            } else {
                log.error("❌ Cannot determine sender: no userId in request and no Principal available");
                return;
            }

            log.debug("WebSocket message received - Sender: {}, Conversation: {}",
                    senderId, request.getConversationId());

            // Save message to database BEFORE broadcast
            // This ensures persistence even if broadcast fails
            Message message = chatService.sendMessage(
                    request.getConversationId(),
                    senderId,
                    request.getContent(),
                    request.getOrgId());

            // Refresh user session (activity detected)
            sessionTracker.refreshSession(request.getConversationId(), senderId);

            // ✅ FIXED: Send TWO messages:
            // 1. Broadcast to OTHER participants (excludes sender)
            messageBroadcaster.broadcastToConversation(message);

            // 2. Send confirmation to SENDER only (so they see their message confirmed)
            messageBroadcaster.sendMessageConfirmationToSender(message);

            log.info("✅ Message handled - ID: {}, Conversation: {}, Sender: {}",
                    message.getId(), request.getConversationId(), senderId);

        } catch (Exception e) {
            log.error("❌ Error handling WebSocket message", e);
            // Send error notification to sender if we have a userId
            try {
                Long senderId = null;
                if (request != null && request.getUserId() != null) {
                    senderId = request.getUserId();
                } else if (principal != null) {
                    senderId = Long.valueOf(principal.getName());
                }

                if (senderId != null) {
                    messageBroadcaster.sendNotification(
                            senderId,
                            "Error",
                            "Failed to send message: " + e.getMessage());
                }
            } catch (Exception notifyError) {
                log.error("Failed to send error notification", notifyError);
            }
        }
    }

    /**
     * Handle typing indicator event
     * Client sends: /app/chat/typing with conversationId, userId
     * Server broadcasts: /topic/conversations/{conversationId}/typing
     * 
     * FIXED: Now uses SimpMessagingTemplate for proper dynamic routing
     */
    @MessageMapping("/chat/typing")
    public void handleTyping(
            ChatPayload.TypingIndicatorRequest request,
            Principal principal) {

        try {
            // Get userId from request (preferred) or Principal (fallback)
            Long userId = null;
            if (request.getUserId() != null) {
                userId = request.getUserId();
            } else if (principal != null) {
                userId = Long.valueOf(principal.getName());
            } else {
                log.warn("⚠️ Cannot determine user for typing indicator: no userId in request and no Principal");
                return;
            }

            log.debug("📝 Typing indicator - User: {}, Conversation: {}",
                    userId, request.getConversationId());

            // Refresh user session (activity detected)
            sessionTracker.refreshSession(request.getConversationId(), userId);

            MessageBroadcaster.PresenceEvent event = MessageBroadcaster.PresenceEvent.builder()
                    .conversationId(request.getConversationId())
                    .userId(userId)
                    .eventType("TYPING")
                    .timestamp(System.currentTimeMillis())
                    .build();

            // Use SimpMessagingTemplate for dynamic routing
            String destination = "/topic/conversations/" + request.getConversationId() + "/typing";
            messagingTemplate.convertAndSend(destination, event);

            log.debug("✅ Typing event broadcasted to {}", destination);

        } catch (Exception e) {
            log.error("❌ Error handling typing indicator", e);
        }
    }

    /**
     * Handle stop typing event
     * Client sends: /app/chat/typing/stop with conversationId, userId
     * Server broadcasts: /topic/conversations/{conversationId}/typing
     * 
     * FIXED: Now uses SimpMessagingTemplate for proper dynamic routing
     */
    @MessageMapping("/chat/typing/stop")
    public void handleStopTyping(
            ChatPayload.TypingIndicatorRequest request,
            Principal principal) {

        try {
            // Get userId from request (preferred) or Principal (fallback)
            Long userId = null;
            if (request.getUserId() != null) {
                userId = request.getUserId();
            } else if (principal != null) {
                userId = Long.valueOf(principal.getName());
            } else {
                log.warn("⚠️ Cannot determine user for stop typing: no userId in request and no Principal");
                return;
            }

            log.debug("⏹️ Stop typing indicator - User: {}, Conversation: {}",
                    userId, request.getConversationId());

            MessageBroadcaster.PresenceEvent event = MessageBroadcaster.PresenceEvent.builder()
                    .conversationId(request.getConversationId())
                    .userId(userId)
                    .eventType("STOP_TYPING")
                    .timestamp(System.currentTimeMillis())
                    .build();

            // Use SimpMessagingTemplate for dynamic routing
            String destination = "/topic/conversations/" + request.getConversationId() + "/typing";
            messagingTemplate.convertAndSend(destination, event);

            log.debug("✅ Stop typing event broadcasted to {}", destination);

        } catch (Exception e) {
            log.error("❌ Error handling stop typing event", e);
        }
    }

    /**
     * Handle user joining conversation
     * Registers session in Redis and broadcasts presence event to all participants
     * Session persists across restarts (with TTL) for online status tracking
     */
    @MessageMapping("/chat/joined/{conversationId}")
    public void handleUserJoined(
            @DestinationVariable Long conversationId,
            ChatPayload.UserPresenceRequest request,
            Principal principal) {

        try {
            // Get userId from request (preferred) or Principal (fallback)
            Long userId = null;
            if (request != null && request.getUserId() != null) {
                userId = request.getUserId();
            } else if (principal != null) {
                userId = Long.valueOf(principal.getName());
            } else {
                log.error("❌ Cannot determine user joined: no userId in request and no Principal available");
                return;
            }

            log.info("✅ User joined conversation - User: {}, Conversation: {}",
                    userId, conversationId);

            // Register session in Redis (persists across restarts)
            sessionTracker.registerSession(conversationId, userId);

            // Broadcast presence event to notify other participants
            messageBroadcaster.broadcastPresenceEvent(
                    conversationId,
                    userId,
                    "JOINED");

        } catch (Exception e) {
            log.error("❌ Error handling user joined event", e);
        }
    }

    /**
     * Handle user leaving conversation
     * Unregisters session from Redis and broadcasts presence event
     */
    @MessageMapping("/chat/left/{conversationId}")
    public void handleUserLeft(
            @DestinationVariable Long conversationId,
            ChatPayload.UserPresenceRequest request,
            Principal principal) {

        try {
            // Get userId from request (preferred) or Principal (fallback)
            Long userId = null;
            if (request != null && request.getUserId() != null) {
                userId = request.getUserId();
            } else if (principal != null) {
                userId = Long.valueOf(principal.getName());
            } else {
                log.error("❌ Cannot determine user left: no userId in request and no Principal available");
                return;
            }

            log.info("✅ User left conversation - User: {}, Conversation: {}",
                    userId, conversationId);

            // Unregister session from Redis
            sessionTracker.unregisterSession(conversationId, userId);

            // Broadcast presence event to notify other participants
            messageBroadcaster.broadcastPresenceEvent(
                    conversationId,
                    userId,
                    "LEFT");

        } catch (Exception e) {
            log.error("❌ Error handling user left event", e);
        }
    }

    /**
     * Get active users in a conversation
     * Returns list of currently online users (persisted in Redis)
     * This survives across server restarts as long as sessions are within TTL
     */
    @MessageMapping("/chat/get-active-users/{conversationId}")
    public void handleGetActiveUsers(
            @DestinationVariable Long conversationId,
            ChatPayload.UserPresenceRequest request,
            Principal principal) {

        try {
            // Get userId from request (preferred) or Principal (fallback)
            Long userId = null;
            if (request != null && request.getUserId() != null) {
                userId = request.getUserId();
            } else if (principal != null) {
                userId = Long.valueOf(principal.getName());
            } else {
                log.error("❌ Cannot get active users: no userId in request and no Principal available");
                return;
            }

            // Get active users from Redis
            var activeUsers = sessionTracker.getActiveUsers(conversationId);

            log.debug("👥 Retrieved {} active users for conversation {}",
                    activeUsers.size(), conversationId);

            // Send list to requesting user
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/active-users",
                    activeUsers);

        } catch (Exception e) {
            log.error("❌ Error getting active users", e);
        }
    }

    /**
     * Health check / ping endpoint
     * Allows clients to test connection and refresh session
     */
    @MessageMapping("/chat/ping")
    public ChatPayload.PingResponse handlePing(Principal principal) {
        try {
            // Refresh session on ping
            if (principal != null) {
                Long userId = Long.valueOf(principal.getName());
                log.debug("🏓 Ping received from user: {}", userId);
            } else {
                log.debug("🏓 Ping received (no user info available)");
            }
        } catch (Exception e) {
            log.debug("⚠️ Error processing ping", e);
        }

        return ChatPayload.PingResponse.builder()
                .timestamp(System.currentTimeMillis())
                .status("PONG")
                .build();
    }
}
