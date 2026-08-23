package com.nexus.cms.chat.model;

import java.util.List;

import com.nexus.cms.chat.entities.ChatMessageAttachment;
import com.nexus.cms.chat.enums.ChatMessageType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatMessageRequestDto {

    @NotNull(message = "Conversation ID is required")
    @Positive(message = "Conversation ID must be positive")
    private Long chatConversationId;

    @NotNull(message = "Participant ID is required")
    @Positive(message = "Participant ID must be positive")
    private Long participantId;

    private Long chatMessageId;

    @NotBlank(message = "Message text cannot be blank for TEXT type messages")
    @Size(max = 5000, message = "Message text cannot exceed 5000 characters")
    private String chatMessageText;

    @Size(max = 10, message = "Cannot attach more than 10 files per message")
    private List<ChatMessageAttachment> chatMessageAttachmentList;

    @NotNull(message = "Message type is required")
    private ChatMessageType chatMessageType;
}
