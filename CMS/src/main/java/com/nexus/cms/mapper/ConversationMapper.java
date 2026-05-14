package com.nexus.cms.mapper;

import com.nexus.cms.model.entities.Conversation;
import com.nexus.cms.model.entities.ConversationParticipant;
import com.nexus.cms.payload.ChatPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between Conversation entities and DTOs
 * Handles denormalization and field mapping
 */
@Component
@RequiredArgsConstructor
public class ConversationMapper {

    /**
     * Convert Conversation entity to ConversationResponse DTO
     */
    public ChatPayload.ConversationResponse toConversationResponse(
            Conversation conversation,
            List<ConversationParticipant> participants) {

        List<ChatPayload.ParticipantInfo> participantInfos = participants.stream()
                .map(this::toParticipantInfo)
                .collect(Collectors.toList());

        return ChatPayload.ConversationResponse.builder()
                .id(conversation.getId())
                .name(conversation.getName())
                .type(conversation.getType())
                .createdBy(conversation.getCreatedBy())
                .orgId(conversation.getOrgId())
                .isActive(conversation.getIsActive())
                .participantCount(conversation.getParticipantCount())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .lastMessageId(conversation.getLastMessageId())
                .lastMessageAt(conversation.getLastMessageAt())
                .participants(participantInfos)
                .build();
    }

    /**
     * Convert Conversation entity to ConversationSummary DTO (for list view)
     */
    public ChatPayload.ConversationSummary toConversationSummary(Conversation conversation) {
        return ChatPayload.ConversationSummary.builder()
                .id(conversation.getId())
                .name(conversation.getName())
                .type(conversation.getType())
                .lastMessage(null) // Would be populated separately if needed
                .lastMessageSenderId(null)
                .lastMessageTime(conversation.getLastMessageAt())
                .unreadCount(0) // Would be calculated separately
                .participantCount((long) conversation.getParticipantCount())
                .build();
    }

    /**
     * Convert ConversationParticipant entity to ParticipantInfo DTO
     */
    public ChatPayload.ParticipantInfo toParticipantInfo(ConversationParticipant participant) {
        return ChatPayload.ParticipantInfo.builder()
                .userId(participant.getUserId())
                .joinedAt(participant.getJoinedAt())
                .isActive(true)
                .build();
    }

    /**
     * Convert CreateConversationRequest to Conversation entity
     */
    public Conversation toConversation(ChatPayload.CreateConversationRequest request, Long creatorUserId) {
        return Conversation.builder()
                .type(request.getType())
                .name(request.getName())
                .createdBy(creatorUserId)
                .orgId(request.getOrgId())
                .isActive(true)
                .participantCount(request.getParticipantIds().size())
                .build();
    }
}
