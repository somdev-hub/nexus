package com.nexus.cms.chat.model;

import com.nexus.cms.chat.enums.ChatConversationType;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class ChatConversationQuickResponseDto {

    private Long chatConversationId;
    private String chatConversationName;
    private String chatConversationAvatar;
    private Timestamp createdAt;
    private Timestamp lastModifiedAt;
    private Long unreadCount;
    private String lastMessage;
    private Timestamp lastMessageAt;
    private Long lastMessageSenderId;
    private String lastMessageSenderName;
    private ChatConversationType  chatConversationType;
    private String otherParticipantName;
    private Long otherParticipantId;
    private String otherParticipantAvatar;

}
