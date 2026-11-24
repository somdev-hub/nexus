package com.nexus.cms.chat.model;

import com.nexus.cms.chat.entities.ChatMessageAttachment;
import com.nexus.cms.chat.enums.ChatConversationType;
import lombok.Data;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Data
public class ChatConversationDetailedSummary {
    private String chatConversationName;
    private String chatConversationAvatar;
    private String chatConversationDescription;
    private ChatConversationType chatConversationType;
    private Boolean isInterOrg;
    private Timestamp createdAt;
    private Long lastModifiedBy;
    private Timestamp lastModifiedAt;
    private Timestamp lastMessageAt;
    private Boolean isActive;
    private Long totalParticipants;
    private Long totalMessages;
    private List<ChatConversationParticipantDto> chatConversationParticipants;
    private List<ChatMessageAttachment> imageAndVideoAttachments = new ArrayList<>();
    private List<ChatMessageAttachment> fileAttachments = new ArrayList<>();
}
