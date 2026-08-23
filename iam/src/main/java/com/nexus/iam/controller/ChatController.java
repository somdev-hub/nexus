package com.nexus.iam.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.iam.service.ChatService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/iam/chat")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/conversations")
    public ResponseEntity<?> createConversation(
            @RequestBody String request,
            @RequestParam Long userId) {
        return chatService.createConversation(request, userId);
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
        return chatService.getUserConversations(page, size, orgId, userId);
    }

    /**
     * Get conversation details by ID
     */
    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<?> getConversation(
            @PathVariable String conversationId,
            @RequestParam Long orgId,
            @RequestParam Long userId
    ) {
       return chatService.getConversation(conversationId, orgId, userId);
    }

    /**
     * Get conversation message history with pagination
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<?> getConversationMessages(
            @PathVariable String conversationId,
            @RequestParam Long orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam Long userId
    ) {
       return chatService.getConversationMessages(conversationId, orgId, page, size, userId);
    }

    /**
     * Add participant to conversation
     */
    @PostMapping("/conversations/{conversationId}/participants")
    public ResponseEntity<?> addParticipant(
            @PathVariable String conversationId,
            @RequestBody String request,
            @RequestParam Long orgId) {
        return chatService.addParticipant(conversationId, request, orgId);
    }

    /**
     * Remove participant from conversation
     */
    @DeleteMapping("/conversations/{conversationId}/participants/{userId}")
    public ResponseEntity<?> removeParticipant(
            @PathVariable String conversationId,
            @PathVariable Long userId,
            @RequestParam Long orgId) {
        return chatService.removeParticipant(conversationId, userId, orgId);
    }

    /**
     * Send message via REST API (alternative to WebSocket)
     * Note: WebSocket/STOMP is preferred for real-time apps
     */
    @PostMapping("/messages")
    public ResponseEntity<?> sendMessage(
            @RequestBody String request,
            @RequestParam Long orgId,
            @RequestParam Long userId
    ) {
        return chatService.sendMessage(request, orgId, userId);
    }

    /**
     * Get specific message by ID
     */
    @GetMapping("/messages/{messageId}")
    public ResponseEntity<?> getMessage(
            @PathVariable String messageId,
            @RequestParam Long orgId) {
        return chatService.getMessage(messageId, orgId);
    }

    /**
     * Get conversation statistics
     */
    @GetMapping("/conversations/{conversationId}/stats")
    public ResponseEntity<?> getConversationStats(
            @PathVariable String conversationId,
            @RequestParam Long orgId) {
        return chatService.getConversationStats(conversationId, orgId);
    }
}
