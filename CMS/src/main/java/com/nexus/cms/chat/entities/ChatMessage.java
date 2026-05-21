package com.nexus.cms.chat.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
@Table(name = "t_chat_conversation_messages", schema = "cms")
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
    @JoinColumn(name = "chat_conversation_participants_chat_conversation_participants_id")
    private ChatConversationParticipant chatConversationParticipant;

    private Boolean isActive;

    @PrePersist
    public void prePersist() {
        this.isActive = true;
    }

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "chatMessage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("message-individual-statuses")
    private List<ChatMessageIndividualStatus> chatMessageIndividualStatuses;
}