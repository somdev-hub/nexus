package com.nexus.nexusbuddy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.nexusbuddy.model.entities.Conversation;
import com.nexus.nexusbuddy.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing chat conversation history.
 * Provides CRUD operations for conversations.
 */
@RestController
@RequestMapping("/nexusbuddy/api/conversations")
@RequiredArgsConstructor
@Slf4j
public class ConversationController {

    private final ConversationRepository conversationRepository;
    private final ObjectMapper objectMapper;

    /**
     * Get all conversations for a user.
     * GET /nexusbuddy/api/conversations?userId=...
     */
    @GetMapping
    public ResponseEntity<?> getConversations(@RequestParam String userId) {
        try {
            List<Conversation> conversations = conversationRepository.findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId);
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            log.error("Failed to get conversations", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get a specific conversation by ID.
     * GET /nexusbuddy/api/conversations/{id}?userId=...
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getConversation(@PathVariable Long id, @RequestParam String userId) {
        try {
            return conversationRepository.findByIdAndUserId(id, userId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Failed to get conversation", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create a new conversation.
     * POST /nexusbuddy/api/conversations
     */
    @PostMapping
    public ResponseEntity<?> createConversation(@RequestBody Map<String, Object> payload) {
        try {
            String userId = (String) payload.get("userId");
            if (userId == null || userId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
            }

            Conversation conversation = Conversation.builder()
                    .userId(userId)
                    .orgId(payload.get("orgId") != null ? Long.parseLong(payload.get("orgId").toString()) : null)
                    .title((String) payload.getOrDefault("title", "New Conversation"))
                    .context((String) payload.get("context"))
                    .messagesJson(payload.get("messagesJson") != null ? payload.get("messagesJson").toString() : "[]")
                    .backendConversationId((String) payload.get("backendConversationId"))
                    .build();

            Conversation saved = conversationRepository.save(conversation);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Failed to create conversation", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update a conversation.
     * PUT /nexusbuddy/api/conversations/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateConversation(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            String userId = (String) payload.get("userId");
            if (userId == null || userId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
            }

            return conversationRepository.findByIdAndUserId(id, userId)
                    .map(existing -> {
                        if (payload.containsKey("title")) {
                            existing.setTitle((String) payload.get("title"));
                        }
                        if (payload.containsKey("messagesJson")) {
                            existing.setMessagesJson(payload.get("messagesJson").toString());
                        }
                        if (payload.containsKey("backendConversationId")) {
                            existing.setBackendConversationId((String) payload.get("backendConversationId"));
                        }
                        if (payload.containsKey("context")) {
                            existing.setContext((String) payload.get("context"));
                        }
                        Conversation saved = conversationRepository.save(existing);
                        return ResponseEntity.ok(saved);
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Failed to update conversation", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete a conversation (soft delete).
     * DELETE /nexusbuddy/api/conversations/{id}?userId=...
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteConversation(@PathVariable Long id, @RequestParam String userId) {
        try {
            return conversationRepository.findByIdAndUserId(id, userId)
                    .map(existing -> {
                        existing.setIsActive(false);
                        conversationRepository.save(existing);
                        return ResponseEntity.ok(Map.of("message", "Conversation deleted"));
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Failed to delete conversation", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}