package com.nexus.iam.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service interface for new Chat Management System (v2)
 * Defines contract for chat operations wrapper methods
 */
public interface NewChatService {

    /**
     * Create a new conversation (DIRECT or GROUP)
     */
    ResponseEntity<?> createConversation(String request, MultipartFile avatar, Long participantId, Long orgId);

    /**
     * Get all conversations for a participant
     */
    ResponseEntity<?> getConversations(Long participantId, Long orgId);

    /**
     * Get full conversation details with participants and attachments
     */
    ResponseEntity<?> getConversationDetails(Long conversationId, Long participantId, Long orgId);

    /**
     * Update conversation details (name, description, avatar)
     */
    ResponseEntity<?> updateConversation(String request, MultipartFile avatar, Long participantId, Long orgId);

    /**
     * Mark conversation as viewed (update lastRead timestamp)
     */
    ResponseEntity<?> markConversationAsViewed(Long participantId, Long conversationId, Long orgId);

    /**
     * Send a text message via REST API
     */
    ResponseEntity<?> sendMessage(String request, Long participantId, Long orgId);

    /**
     * Upload multimedia files (images, videos, documents)
     */
    ResponseEntity<?> uploadMultimedia(MultipartFile[] files, Long participantId, Long orgId);

    /**
     * Edit an existing message
     */
    ResponseEntity<?> editMessage(String request, Long participantId, Long orgId);

    /**
     * Get conversation messages with cursor-based pagination
     */
    ResponseEntity<?> getConversationMessages(Long conversationId, Long participantId, Long beforeId, Long limit,
            Long orgId);
}
