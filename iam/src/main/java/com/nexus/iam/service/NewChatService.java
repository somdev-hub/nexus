package com.nexus.iam.service;

import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service interface for new Chat Management System (v2)
 * Defines contract for chat operations wrapper methods
 */
public interface NewChatService {

    /**
     * Create a new conversation (DIRECT or GROUP)
     */
    ResponseEntity<?> createConversation(String request, MultipartFile avatar);

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
    ResponseEntity<?> sendMessage(Object request, List<MultipartFile> files, Long participantId, Long orgId);

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

    /**
     * Get online/offline presence statuses for a batch of users.
     */
    ResponseEntity<?> getPresenceStatuses(java.util.List<Long> userIds);

    ResponseEntity<?> addParticipants(Long conversationId, String request, Long participantId, Long orgId);

    ResponseEntity<?> removeParticipant(Long conversationId, Long participantIdToRemove, Long participantId, Long orgId);

    ResponseEntity<?> setAdmin(Long conversationId, Long participantId, Long adminId, boolean isAdmin, Long orgId);

    ResponseEntity<?> deleteConversation(Long conversationId, Long participantId, Long orgId);

    ResponseEntity<?> clearChatForSelf(Long conversationId, Long participantId, Long orgId);

    ResponseEntity<?> downloadChat(Long conversationId, Long participantId, Long orgId);

    ResponseEntity<?> searchMessages(Long conversationId, Long participantId, String query, Long orgId);

    /**
     * Delete a message
     */
    ResponseEntity<?> deleteMessage(Long messageId, Long participantId, Long orgId, boolean deleteForEveryone);

    /**
     * Pin a message
     */
    ResponseEntity<?> pinMessage(Long messageId, Long participantId, Long orgId);

    /**
     * Unpin a message
     */
    ResponseEntity<?> unpinMessage(Long messageId, Long participantId, Long orgId);

    /**
     * Get pinned message for a conversation
     */
    ResponseEntity<?> getPinnedMessage(Long conversationId, Long participantId, Long orgId);

    /**
     * Send a message with optional reply reference
     */
    ResponseEntity<?> sendMessageWithReply(Long conversationId, Long participantId, Long orgId, String content, Long repliedToMessageId);
}
