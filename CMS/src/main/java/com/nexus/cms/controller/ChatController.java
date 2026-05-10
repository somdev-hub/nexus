package com.nexus.cms.controller;

import com.nexus.cms.model.entities.Conversation;
import com.nexus.cms.model.entities.Message;
import com.nexus.cms.payload.ChatPayload;
import com.nexus.cms.service.ChatService;
import com.nexus.cms.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST API Controller for Chat Operations
 *
 * Endpoints:
 * - POST /cms/chat/conversations - Create new conversation
 * - GET /cms/chat/conversations - List user's conversations
 * - GET /cms/chat/conversations/{id} - Get conversation details
 * - GET /cms/chat/conversations/{id}/messages - Get message history
 * - POST /cms/chat/conversations/{id}/participants - Add participant
 * - DELETE /cms/chat/conversations/{id}/participants/{userId} - Remove participant
 * - POST /cms/chat/messages - Send message (REST alternative to WebSocket)
 * - GET /cms/chat/messages/{id} - Get specific message
 */
@Slf4j
@RestController
@RequestMapping("/cms/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ConversationService conversationService;

    /**
     * Create a new conversation (1-1 or group)
     */
    @PostMapping("/conversations")
    public ResponseEntity<?> createConversation(
            @RequestBody ChatPayload.CreateConversationRequest request,
            @RequestParam Long userId) {
        try {
//            Long userId = Long.valueOf(authentication.getName());

            Conversation conversation = conversationService.createConversation(
                    request.getType(),
                    request.getName(),
                    userId,
                    request.getParticipantIds(),
                    request.getOrgId()
            );

            log.info("Conversation created - ID: {}, Type: {}", conversation.getId(), conversation.getType());
            return ResponseEntity.status(HttpStatus.CREATED).body(conversation);

        } catch (Exception e) {
            log.error("Error creating conversation", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ChatPayload.ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get user's conversations with pagination
     */
    @GetMapping("/conversations")
    public ResponseEntity<?> getUserConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam Long orgId,
            @RequestParam Long userId) {
        try {
//            Long userId = Long.valueOf(authentication.getName());
            Pageable pageable = PageRequest.of(page, size);

            Page<Conversation> conversations = conversationService.getUserConversations(
                    userId,
                    orgId,
                    pageable
            );

            return ResponseEntity.ok(conversations);

        } catch (Exception e) {
            log.error("Error fetching user conversations", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ChatPayload.ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get conversation details by ID
     */
    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<?> getConversation(
            @PathVariable UUID conversationId,
            @RequestParam Long orgId,
            @RequestParam Long userId
            ) {
        try {
//            Long userId = Long.valueOf(authentication.getName());

            Conversation conversation = conversationService.getConversation(conversationId, orgId);

            // Verify user is participant
            if (!conversationService.isUserParticipant(conversationId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ChatPayload.ErrorResponse("User is not a participant of this conversation"));
            }

            return ResponseEntity.ok(conversation);

        } catch (Exception e) {
            log.error("Error fetching conversation", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ChatPayload.ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get conversation message history with pagination
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<?> getConversationMessages(
            @PathVariable UUID conversationId,
            @RequestParam Long orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam Long userId
            ) {
        try {
//            Long userId = Long.valueOf(authentication.getName());
            Pageable pageable = PageRequest.of(page, size);

            Page<Message> messages = chatService.getConversationHistory(
                    conversationId,
                    orgId,
                    userId,
                    pageable
            );

            return ResponseEntity.ok(messages);

        } catch (Exception e) {
            log.error("Error fetching conversation messages", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ChatPayload.ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Add participant to conversation
     */
    @PostMapping("/conversations/{conversationId}/participants")
    public ResponseEntity<?> addParticipant(
            @PathVariable UUID conversationId,
            @RequestBody ChatPayload.AddParticipantRequest request,
            @RequestParam Long orgId) {
        try {
            conversationService.addParticipant(conversationId, request.getUserId(), orgId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ChatPayload.SuccessResponse("Participant added successfully"));

        } catch (Exception e) {
            log.error("Error adding participant", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ChatPayload.ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Remove participant from conversation
     */
    @DeleteMapping("/conversations/{conversationId}/participants/{userId}")
    public ResponseEntity<?> removeParticipant(
            @PathVariable UUID conversationId,
            @PathVariable Long userId,
            @RequestParam Long orgId) {
        try {
            conversationService.removeParticipant(conversationId, userId, orgId);
            return ResponseEntity.ok(new ChatPayload.SuccessResponse("Participant removed successfully"));

        } catch (Exception e) {
            log.error("Error removing participant", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ChatPayload.ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Send message via REST API (alternative to WebSocket)
     * Note: WebSocket/STOMP is preferred for real-time apps
     */
    @PostMapping("/messages")
    public ResponseEntity<?> sendMessage(
            @RequestBody ChatPayload.SendMessageRequest request,
            @RequestParam Long orgId,
            @RequestParam Long userId
            ) {
        try {
//            Long senderId = Long.valueOf(authentication.getName());

            Message message = chatService.sendMessage(
                    request.getConversationId(),
                    userId,
                    request.getContent(),
                    orgId
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(message);

        } catch (Exception e) {
            log.error("Error sending message", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ChatPayload.ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get specific message by ID
     */
    @GetMapping("/messages/{messageId}")
    public ResponseEntity<?> getMessage(
            @PathVariable UUID messageId,
            @RequestParam Long orgId) {
        try {
            Message message = chatService.getMessage(messageId, orgId);
            return ResponseEntity.ok(message);

        } catch (Exception e) {
            log.error("Error fetching message", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ChatPayload.ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get conversation statistics
     */
    @GetMapping("/conversations/{conversationId}/stats")
    public ResponseEntity<?> getConversationStats(
            @PathVariable UUID conversationId,
            @RequestParam Long orgId) {
        try {
            Conversation conversation = conversationService.getConversation(conversationId, orgId);
            long totalMessages = chatService.getConversationMessageCount(conversationId);
            long deliveredMessages = chatService.getDeliveredMessageCount(conversationId);

            ChatPayload.ConversationStats stats = ChatPayload.ConversationStats.builder()
                    .conversationId(conversationId)
                    .totalMessages(totalMessages)
                    .deliveredMessages(deliveredMessages)
                    .participantCount((long) conversation.getParticipants().size())
                    .createdAt(conversation.getCreatedAt())
                    .build();

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error fetching conversation stats", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ChatPayload.ErrorResponse(e.getMessage()));
        }
    }
}

