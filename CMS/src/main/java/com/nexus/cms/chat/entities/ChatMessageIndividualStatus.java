package com.nexus.cms.chat.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.nexus.cms.chat.enums.ChatMessageStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.sql.Timestamp;

@Entity
@Data
@Table(name = "t_chat_message_status", schema = "cms")
public class ChatMessageIndividualStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatMessageIndividualStatusId;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonBackReference("message-individual-status")
    @ManyToOne
    @JoinColumn(name = "chat_message_chat_message_id")
    private ChatMessage chatMessage;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonBackReference("participant-individual-status")
    @ManyToOne
    @JoinColumn(name = "participant_chat_conversation_participants_id")
    private ChatConversationParticipant participant;

    @Enumerated(EnumType.STRING)
    private ChatMessageStatus status;

    private Timestamp deliveredAt;

    private Timestamp receivedAt;
}
