package com.nexus.cms.chat.service.interfaces;

import com.nexus.cms.chat.entities.ChatMessageIndividualStatus;
import com.nexus.cms.chat.model.ChatConversationParticipantDto;
import com.nexus.cms.chat.model.ConversationRequestDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface NewChatService {
    ResponseEntity<?> createChatConversation(@Valid ConversationRequestDto request, MultipartFile chatConversationAvatar);

    ResponseEntity<?> getChatConversationMessages(Long participantId);

    ResponseEntity<?> getChatConversationMessages(Long conversationId, Long participantId, Long beforeId, Long limit);

    ResponseEntity<?> getChatConversation(Long conversationId, Long participantId);

    ResponseEntity<?> updateChatConversation(@Valid ConversationRequestDto request, MultipartFile chatConversationAvatar);

    ResponseEntity<?> addParticipants(Long conversationId, java.util.List<ChatConversationParticipantDto> participants);

    ResponseEntity<?> removeParticipant(Long conversationId, Long participantId);

    ResponseEntity<?> setAdmin(Long conversationId, Long participantId, Long adminId, boolean isAdmin);

    ResponseEntity<?> viewConversation(Long participantId, Long conversationId);

    ResponseEntity<ChatMessageIndividualStatus> markReceived(Long messageId, Long participantId);

    ResponseEntity<?> deleteConversation(Long conversationId, Long participantId);

    ResponseEntity<?> clearChatForSelf(Long conversationId, Long participantId);

    ResponseEntity<?> downloadChat(Long conversationId, Long participantId);

    ResponseEntity<?> searchMessages(Long conversationId, Long participantId, String query);

    /**
     * Delete a message
     * @param messageId        Message ID to delete
     * @param participantId    ID of user requesting deletion
     * @param deleteForEveryone If true, delete for all participants; if false, delete only for requester
     */
    ResponseEntity<?> deleteMessage(Long messageId, Long participantId, boolean deleteForEveryone);

    /**
     * Pin a message in a conversation
     */
    ResponseEntity<?> pinMessage(Long messageId, Long participantId);

    /**
     * Unpin a message in a conversation
     */
    ResponseEntity<?> unpinMessage(Long messageId, Long participantId);

    /**
     * Get pinned message for a conversation
     */
    ResponseEntity<?> getPinnedMessage(Long conversationId, Long participantId);

    /**
     * Send a message with optional reply reference
     */
    ResponseEntity<?> sendMessageWithReply(Long conversationId, Long participantId, String content, Long repliedToMessageId);
}
