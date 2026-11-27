package com.nexus.cms.chat.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nexus.cms.chat.enums.ChatMessageStatus;
import com.nexus.cms.chat.enums.ChatMessageType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.util.List;

@Data
@Entity
@Table(name = "t_chat_conversation_messages", schema = "cms", indexes = {
        @Index(name = "idx_chat_msg_conv", columnList = "chat_conversation_chat_conversation_id"),
        @Index(name = "idx_chat_msg_status", columnList = "chat_message_status")
})
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatMessageId;

    @Column(columnDefinition = "TEXT")
    private String chatMessageText;

    @Enumerated(EnumType.STRING)
    private ChatMessageType chatMessageType;

    @Enumerated(EnumType.STRING)
    private ChatMessageStatus chatMessageStatus;

    @CreationTimestamp
    private Timestamp sentAt;

    private Timestamp deliveredAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    @JsonProperty("isEdited")
    private Boolean isEdited;

    private Timestamp receivedAt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonManagedReference("message-attachments")
    @OneToMany(mappedBy = "chatMessage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ChatMessageAttachment> chatMessageAttachmentList;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonBackReference("conversation-messages")
    @ManyToOne
    @JoinColumn(name = "chat_conversation_chat_conversation_id")
    private ChatConversation chatConversation;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "chat_conversation_participant_id")
    private ChatConversationParticipant chatConversationParticipant;

    private Boolean isActive;

    @Version
    private Long version;

    @Column(name = "client_message_id", length = 64)
    private String clientMessageId;

    @Column(name = "replied_to_message_id")
    private Long repliedToMessageId;

    @Column(name = "is_pinned")
    @JsonProperty("isPinned")
    private Boolean isPinned = false;

    @Column(name = "pinned_at")
    private Timestamp pinnedAt;

    @Column(name = "pinned_by")
    private Long pinnedBy;

    @Column(name = "is_deleted")
    @JsonProperty("isDeleted")
    private Boolean isDeleted = false;

    @Column(name = "deleted_for_everyone")
    @JsonProperty("deletedForEveryone")
    private Boolean deletedForEveryone = false;

    @PrePersist
    public void prePersist() {
        this.isActive = true;
        if (this.version == null) {
            this.version = 0L;
        }
        if (this.isPinned == null) {
            this.isPinned = false;
        }
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
        if (this.deletedForEveryone == null) {
            this.deletedForEveryone = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        if (this.version == null) {
            this.version = 0L;
        }
        if (this.isPinned == null) {
            this.isPinned = false;
        }
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
        if (this.deletedForEveryone == null) {
            this.deletedForEveryone = false;
        }
    }

    @PostLoad
    public void postLoad() {
        if (this.isPinned == null) {
            this.isPinned = false;
        }
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
        if (this.deletedForEveryone == null) {
            this.deletedForEveryone = false;
        }
    }

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "chatMessage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("message-individual-statuses")
    private List<ChatMessageIndividualStatus> chatMessageIndividualStatuses;
}