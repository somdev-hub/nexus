package com.nexus.cms.chat.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.nexus.cms.chat.enums.ChatConversationType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "t_chat_conversations", schema = "cms", indexes = {
        @Index(name = "idx_chat_conv_last_message_at", columnList = "last_message_at"),
        @Index(name = "idx_chat_conv_org_id", columnList = "org_id")
})
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatConversationId;

    @Column(columnDefinition = "TEXT")
    private String chatConversationName;

    private String chatConversationAvatar;

    @Column(columnDefinition = "TEXT")
    private String chatConversationDescription;

    @Enumerated(EnumType.STRING)
    private ChatConversationType chatConversationType;

    @CreationTimestamp
    private Timestamp createdAt;

    private Long lastModifiedBy;

    @UpdateTimestamp
    private Timestamp lastModifiedAt;

    private Timestamp lastMessageAt;

    @Column(name = "last_message_text", columnDefinition = "TEXT")
    private String lastMessageText;

    @Column(name = "last_message_sender_id")
    private Long lastMessageSenderId;

    @Column(name = "last_message_sender_name")
    private String lastMessageSenderName;

    private Boolean isActive;

    private Long totalParticipants;

    private Long totalMessages;

    @Column(name = "org_id")
    private Long orgId; // Creator's org (nullable for inter-org conversations)

    @Column(name = "is_inter_org")
    private Boolean isInterOrg; // True if participants span multiple orgs

    @Column(name = "participant_org_ids", columnDefinition = "TEXT")
    private String participantOrgIds; // JSON array of all participant org IDs

    @PrePersist
    public void prePersist() {
        this.isActive = true;
        if (this.isInterOrg == null) {
            this.isInterOrg = false;
        }
    }

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonManagedReference("conversation-participants")
    @OneToMany(mappedBy = "chatConversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ChatConversationParticipant> chatConversationParticipants = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonManagedReference("conversation-messages")
    @OneToMany(mappedBy = "chatConversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ChatMessage> chatMessages = new ArrayList<>();
}
