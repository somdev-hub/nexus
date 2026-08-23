package com.nexus.cms.chat.model;

import com.nexus.cms.chat.entities.ChatMessageAttachment;
import com.nexus.cms.chat.enums.ChatMessageStatus;
import com.nexus.cms.chat.enums.ChatMessageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Data
public class ConversationMessagesDto {
    private Long chatMessageId;
    private String chatMessageText;
    private ChatMessageType chatMessageType;
    private ChatMessageStatus chatMessageStatus;
    private Timestamp sentAt;
    private Timestamp deliveredAt;
    private Timestamp updatedAt;
    private Timestamp receivedAt;
    private List<ChatMessageAttachment> chatMessageAttachmentList;
    private List<MessageSeenBy> messageSeenByList;
    private ParticipantInfo chatConversationParticipant;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ParticipantInfo {
        private Long participantId;
        private String participantName;
        private String participantEmail;
        private String participantAvatar;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MessageSeenBy {
        private Long participantId;
        private String participantName;
        private String participantAvatar;
    }
}
