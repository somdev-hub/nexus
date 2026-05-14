package com.nexus.cms.controller;

import com.nexus.cms.exception.ResourceNotFoundException;
import com.nexus.cms.exception.ServiceLevelException;
import com.nexus.cms.mapper.ConversationMapper;
import com.nexus.cms.model.entities.Conversation;
import com.nexus.cms.model.entities.ConversationParticipant;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller for Chat Operations
 *
 * Endpoints:
 * - POST /cms/chat/conversations - Create new conversation
 * - GET /cms/chat/conversations - List user's conversations
 * - GET /cms/chat/conversations/{id} - Get conversation details
 * - GET /cms/chat/conversations/{id}/messages - Get message history
 * - GET /cms/chat/conversations/{id}/participants - Get conversation
 * participants
 * - POST /cms/chat/conversations/{id}/participants - Add participant
 * - DELETE /cms/chat/conversations/{id}/participants/{userId} - Remove
 * participant
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
    private final ConversationMapper conversationMapper;

    /**
     * Create a new conversation (1-1 or group)
     * Creates conversation with participants atomically to avoid transaction issues
     */
    @PostMapping("/conversations")
    public ResponseEntity<?> createConversation(
            @RequestBody ChatPayload.CreateConversationRequest request,
            @RequestParam Long userId) {
        try {
            log.info("Creating conversation - Type: {}, Creator: {}, Org: {}",
                    request.getType(), userId, request.getOrgId());

            Conversation conversation = conversationService.createConversation(
                    request.getType(),
                    request.getName(),
                    userId,
                    request.getParticipantIds(),
                    request.getOrgId());

            // Fetch participants for response
            List<ConversationParticipant> participants = conversationService.getParticipants(conversation.getId());
            ChatPayload.ConversationResponse response = conversationMapper.toConversationResponse(conversation,
                    participants);

            log.info("Conversation created successfully - ID: {}", conversation.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (ServiceLevelException e) {
            log.warn("Service error creating conversation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ChatPayload.ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error creating conversation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChatPayload.ErrorResponse("Failed to create conversation: " + e.getMessage()));
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
            Pageable pageable = PageRequest.of(page, size);

            Page<Conversation> conversations = conversationService.getUserConversations(
                    userId,
                    orgId,
                    pageable);

            // Map to summary DTOs
            Page<ChatPayload.ConversationSummary> summaries = conversations
                    .map(conversationMapper::toConversationSummary);

            log.info("Fetched {} conversations for user {}", conversations.getTotalElements(), userId);
            return ResponseEntity.ok(summaries);

        } catch (ServiceLevelException e) {
            log.warn("Service error fetching conversations: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ChatPayload.ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error fetching conversations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChatPayload.ErrorResponse("Failed to fetch conversations"));
        }
    }

    /**
     * Get conversation details by ID with participant verification
     */
    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<?> getConversation(
            @PathVariable Long conversationId,
            @RequestParam Long orgId,
            @RequestParam Long userId) {
        try {
            Conversation conversation = conversationService.getConversation(conversationId, orgId);

            // Verify user is participant
            if (!conversationService.isUserParticipant(conversationId, userId)) {
                log.warn("Unauthorized access attempt - User {} not participant of conversation {}", userId,
                        conversationId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ChatPayload.ErrorResponse("User is not a participant of this conversation"));
            }

            List<ConversationParticipant> participants = conversationService.getParticipants(conversationId);
            ChatPayload.ConversationResponse response = conversationMapper.toConversationResponse(conversation,
                    participants);

            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            log.warn("Conversation not found: {}", conversationId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ChatPayload.ErrorResponse("Conversation not found"));
        } catch (ServiceLevelException e) {
            log.warn("Service error fetching conversation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ChatPayload.ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error fetching conversation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChatPayload.ErrorResponse("Failed to fetch conversation"));
        }
    }

    /**
     * Get conversation participants
     */
    @GetMapping("/conversations/{conversationId}/participants")
    public ResponseEntity<?> getConversationParticipants(
            @PathVariable Long conversationId,
            @RequestParam Long orgId,
            @RequestParam Long userId) {
        try {
            // Verify conversation exists and user is participant
            conversationService.getConversation(conversationId, orgId);
            if (!conversationService.isUserParticipant(conversationId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ChatPayload.ErrorResponse("User is not a participant of this conversation"));
            }

            List<ConversationParticipant> participants = conversationService.getParticipants(conversationId);
            List<ChatPayload.ParticipantInfo> participantInfos = participants.stream()
                    .map(conversationMapper::toParticipantInfo)
                    .toList();

            return ResponseEntity.ok(participantInfos);

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ChatPayload.ErrorResponse("Conversation not found"));
        } catch (Exception e) {
            log.error("Error fetching participants", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChatPayload.ErrorResponse("Failed to fetch participants"));
        }
    }

    /**
     * Get conversation message history with pagination
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<?> getConversationMessages(
            @PathVariable Long conversationId,
            @RequestParam Long orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam Long userId) {
        try {
            // Verify user is participant
            conversationService.getConversation(conversationId, orgId);
            if (!conversationService.isUserParticipant(conversationId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ChatPayload.ErrorResponse("User is not a participant of this conversation"));
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<Message> messages = chatService.getConversationHistory(conversationId, orgId, userId, pageable);

            return ResponseEntity.ok(messages);

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ChatPayload.ErrorResponse("Conversation not found"));
        } catch (ServiceLevelException e) {
            log.warn("Service error fetching messages: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ChatPayload.ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error fetching messages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChatPayload.ErrorResponse("Failed to fetch messages"));
        }
    }

    /**
     * Add participant to conversation
     */
    @PostMapping("/conversations/{conversationId}/participants")
    public ResponseEntity<?> addParticipant(
            @PathVariable Long conversationId,
            @RequestBody ChatPayload.AddParticipantRequest request,
            @RequestParam Long orgId) {
        try {
            conversationService.addParticipant(conversationId, request.getUserId(), orgId);
            log.info("Participant {} added to conversation {}", request.getUserId(), conversationId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ChatPayload.SuccessResponse("Participant added successfully"));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ChatPayload.ErrorResponse("Conversation not found"));
        } catch (ServiceLevelException e) {
            log.warn("Service error adding participant: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ChatPayload.ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error adding participant", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChatPayload.ErrorResponse("Failed to add participant"));
        }
    }

    /**
     * Remove participant from conversation
     */
    @DeleteMapping("/conversations/{conversationId}/participants/{userId}")
    public ResponseEntity<?> removeParticipant(
            @PathVariable Long conversationId,
            @PathVariable Long userId,
            @RequestParam Long orgId) {
        try {
            conversationService.removeParticipant(conversationId, userId, orgId);
            log.info("Participant {} removed from conversation {}", userId, conversationId);
            return ResponseEntity.ok(new ChatPayload.SuccessResponse("Participant removed successfully"));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ChatPayload.ErrorResponse("Conversation or participant not found"));
        } catch (ServiceLevelException e) {
            log.warn("Service error removing participant: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ChatPayload.ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error removing participant", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChatPayload.ErrorResponse("Failed to remove participant"));
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
            @RequestParam Long userId) {
        try {
            // Long senderId = Long.valueOf(authentication.getName());

            Message message = chatService.sendMessage(
                    request.getConversationId(),
                    userId,
                    request.getContent(),
                    orgId);

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
            @PathVariable Long messageId,
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
            @PathVariable Long conversationId,
            @RequestParam Long orgId) {
        try {
            Conversation conversation = conversationService.getConversation(conversationId, orgId);
            long totalMessages = chatService.getConversationMessageCount(conversationId);
            long deliveredMessages = chatService.getDeliveredMessageCount(conversationId);

            ChatPayload.ConversationStats stats = ChatPayload.ConversationStats.builder()
                    .conversationId(conversationId)
                    .totalMessages(totalMessages)
                    .deliveredMessages(deliveredMessages)
                    .participantCount((long) conversation.getParticipantCount())
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
