package com.nexus.cms.chat.entities;

import com.nexus.cms.chat.enums.ChatParticipantCurrentStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

@Entity
@Data
@Table(name = "t_chat_participants_presence", schema = "cms")
public class ChatParticipantsPresence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatParticipantsPresenceId;

    private Long userId;

    private Boolean isOnline;

    private Timestamp lastActiveAt;
}
