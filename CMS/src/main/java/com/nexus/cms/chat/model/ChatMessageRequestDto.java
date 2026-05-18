package com.nexus.cms.chat.model;

import com.nexus.cms.chat.enums.ChatMessageType;
import lombok.Data;
import lombok.NonNull;

@Data
public class ChatMessageRequestDto {

    private Long chatConversationId;
    private Long participantId;

    private Long chatMessageId;
    private String chatMessageText;
    @NonNull
    private ChatMessageType chatMessageType;
}
