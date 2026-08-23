package com.nexus.cms.chat.model;

import com.nexus.cms.chat.enums.ChatConversationType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class ConversationRequestDto {

    private Long chatConversationId;

    @Size(min = 2, max = 100, message = "Conversation name must be between 2 and 100 characters")
    private String chatConversationName;

    private String chatConversationAvatar;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String chatConversationDescription;

    @NotNull(message = "Organization ID is required")
    @Positive(message = "Organization ID must be positive")
    private Long orgId;

    @NotNull(message = "Conversation type is required")
    private ChatConversationType chatConversationType;

    @NotEmpty(message = "At least one participant is required")
    @Size(min = 1, max = 100, message = "Conversation cannot have more than 100 participants")
    private List<ChatConversationParticipantDto> chatConversationParticipants;

    private Boolean isActive;
}
