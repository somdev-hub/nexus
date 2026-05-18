package com.nexus.cms.chat.model;

import com.nexus.cms.chat.enums.ChatConversationType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ConversationRequestDto {

    private Long chatConversationId;

    private String chatConversationName;

    private String chatConversationAvatar;

    private String chatConversationDescription;

    private Long orgId;

    @NotNull
    private ChatConversationType chatConversationType;

    private List<ChatConversationParticipantDto> chatConversationParticipants;

    private Boolean isActive;
}
