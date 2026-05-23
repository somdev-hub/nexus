package com.nexus.iam.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.nexus.iam.service.NewChatService;

/**
 * Wrapper endpoints for new Chat Management System (v2)
 * Acts as gateway for chat operations with authentication and authorization
 * checks
 * All requests are routed through IAM gateway before reaching CMS microservice
 */
@RestController
@RequestMapping("/iam/chat/v2")
@RequiredArgsConstructor
public class NewChatController {

    private final NewChatService newChatService;

    /**
     * Create a new conversation (DIRECT or GROUP)
     * 
     * @param request       MultipartFile containing JSON request and optional
     *                      avatar file
     * @param participantId Current participant ID (from JWT token)
     * @param orgId         Organization ID
     * @return ResponseEntity with created conversation details
     */
    @PostMapping("/conversation")
    public ResponseEntity<?> createConversation(
            @RequestParam("request") String request,
            @RequestParam(value = "chatConversationAvatar", required = false) MultipartFile avatar,
            @RequestParam Long participantId,
            @RequestParam Long orgId) {
        return newChatService.createConversation(request, avatar, participantId, orgId);
    }

    /**
     * Get all conversations for a participant (inbox view)
     * 
     * @param participantId Current participant ID
     * @param orgId         Organization ID
     * @return ResponseEntity with list of conversations
     */
    @GetMapping("/conversations/{participantId}")
    public ResponseEntity<?> getConversations(
            @PathVariable Long participantId,
            @RequestParam Long orgId) {
        return newChatService.getConversations(participantId, orgId);
    }

    /**
     * Get full conversation details with participants and attachments
     * 
     * @param conversationId Conversation ID
     * @param participantId  Current participant ID
     * @param orgId          Organization ID
     * @return ResponseEntity with conversation details
     */
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<?> getConversationDetails(
            @PathVariable Long conversationId,
            @RequestParam Long participantId,
            @RequestParam Long orgId) {
        return newChatService.getConversationDetails(conversationId, participantId, orgId);
    }

    /**
     * Update conversation details (name, description, avatar)
     * 
     * @param request       MultipartFile containing JSON request and optional new
     *                      avatar
     * @param participantId Current participant ID
     * @param orgId         Organization ID
     * @return ResponseEntity with updated conversation
     */
    @PutMapping("/conversation")
    public ResponseEntity<?> updateConversation(
            @RequestParam("request") String request,
            @RequestParam(value = "chatConversationAvatar", required = false) MultipartFile avatar,
            @RequestParam Long participantId,
            @RequestParam Long orgId) {
        return newChatService.updateConversation(request, avatar, participantId, orgId);
    }

    /**
     * Mark conversation as viewed (update lastRead timestamp)
     * 
     * @param participantId  Current participant ID
     * @param conversationId Conversation ID to mark as viewed
     * @param orgId          Organization ID
     * @return ResponseEntity with status
     */
    @PostMapping("/view")
    public ResponseEntity<?> markConversationAsViewed(
            @RequestParam Long participantId,
            @RequestParam Long conversationId,
            @RequestParam Long orgId) {
        return newChatService.markConversationAsViewed(participantId, conversationId, orgId);
    }

    /**
     * Send a text message via REST API
     * 
     * @param request       MultipartFile containing JSON message data
     * @param participantId Current participant ID (sender)
     * @param orgId         Organization ID
     * @return ResponseEntity with sent message details
     */
    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(
            @RequestParam("message") String request,
            @RequestParam Long participantId,
            @RequestParam Long orgId) {
        return newChatService.sendMessage(request, participantId, orgId);
    }

    /**
     * Upload multimedia files (images, videos, documents)
     * Pre-upload media before including in message
     * 
     * @param files         Array of files to upload
     * @param participantId Current participant ID
     * @param orgId         Organization ID
     * @return ResponseEntity with attachment metadata list
     */
    @PostMapping("/message/multimedia")
    public ResponseEntity<?> uploadMultimedia(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam Long participantId,
            @RequestParam Long orgId) {
        return newChatService.uploadMultimedia(files, participantId, orgId);
    }

    /**
     * Edit an existing message
     * 
     * @param request       JSON containing message ID and new text
     * @param participantId Current participant ID (message sender)
     * @param orgId         Organization ID
     * @return ResponseEntity with updated message details
     */
    @PutMapping("/message")
    public ResponseEntity<?> editMessage(
            @RequestBody String request,
            @RequestParam Long participantId,
            @RequestParam Long orgId) {
        return newChatService.editMessage(request, participantId, orgId);
    }

    /**
     * Get conversation messages with cursor-based pagination
     * 
     * @param conversationId Conversation ID
     * @param participantId  Current participant ID
     * @param beforeId       Message ID cursor (for pagination)
     * @param limit          Number of messages to fetch (default: 50)
     * @param orgId          Organization ID
     * @return ResponseEntity with paginated messages
     */
    @GetMapping("/conversation/messages")
    public ResponseEntity<?> getConversationMessages(
            @RequestParam Long conversationId,
            @RequestParam Long participantId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "50") Long limit,
            @RequestParam Long orgId) {
        return newChatService.getConversationMessages(conversationId, participantId, beforeId, limit, orgId);
    }
}
