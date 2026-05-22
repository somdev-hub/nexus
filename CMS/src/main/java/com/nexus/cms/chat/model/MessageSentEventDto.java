package com.nexus.cms.chat.model;

import com.nexus.cms.chat.entities.ChatMessage;
import com.nexus.cms.chat.enums.ChatMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageSentEventDto {
    private Long messageId;
    private Long conversationId;
    private Long participantId;
    private ChatMessage message;
}
