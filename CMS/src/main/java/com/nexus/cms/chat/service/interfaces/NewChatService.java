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
}
