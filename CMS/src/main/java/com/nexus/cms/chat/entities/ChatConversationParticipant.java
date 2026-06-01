package com.nexus.cms.chat.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.nexus.cms.chat.enums.ChatParticipantCurrentStatus;
import com.nexus.cms.chat.enums.ChatParticipantStatus;
import com.nexus.cms.chat.enums.ChatParticipantType;
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
@Table(name = "t_chat_conversation_participants", schema = "cms")
public class ChatConversationParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatConversationParticipantsId;

    private Long participantId;

    private String participantName;

    private String participantEmail;

    private String participantMob;

    private String participantRole;

    private String participantAvatar;

    private Boolean isChatCreator;

    @Enumerated(EnumType.STRING)
    private ChatParticipantType chatParticipantType;

    @CreationTimestamp
    private Timestamp joinedAt;

    private Timestamp lastSeenAt;

    @Enumerated(EnumType.STRING)
    private ChatParticipantStatus chatParticipantStatus;

    @Enumerated(EnumType.STRING)
    private ChatParticipantCurrentStatus chatParticipantCurrentStatus;

    @UpdateTimestamp
    private Timestamp updatedAt;

    private Boolean isActive;

    private Timestamp lastRead;

    private Long lastMessageId;

    @PrePersist
    public void prePersist() {
        this.isActive = true;
    }

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonBackReference("conversation-participants")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_conversation_id", nullable = false)
    private ChatConversation chatConversation;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonBackReference("participant-messages")
    @OneToMany(mappedBy = "participant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ChatMessageIndividualStatus> chatMessageIndividualStatuses;
}