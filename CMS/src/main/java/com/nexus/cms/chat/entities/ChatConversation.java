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
@Table(name = "t_chat_conversations", schema = "cms")
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

    private Boolean isActive;

    private Long totalParticipants;

    private Long totalMessages;

    private Long orgId;

    @PrePersist
    public void prePersist() {
        this.isActive = true;
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
