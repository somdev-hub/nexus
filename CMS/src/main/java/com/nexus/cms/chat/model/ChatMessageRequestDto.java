package com.nexus.cms.chat.model;

import com.nexus.cms.chat.entities.ChatMessageAttachment;
import com.nexus.cms.chat.enums.ChatMessageType;
import lombok.Data;
import lombok.NonNull;

import java.util.List;

@Data
public class ChatMessageRequestDto {

    private Long chatConversationId;
    private Long participantId;

    private Long chatMessageId;
    private String chatMessageText;

    private List<ChatMessageAttachment> chatMessageAttachmentList;

    @NonNull
    private ChatMessageType chatMessageType;
}
