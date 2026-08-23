package com.nexus.cms.chat.entities;

import java.sql.Timestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "t_chat_participants_presence", schema = "cms")
public class ChatParticipantsPresence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatParticipantsPresenceId;

    private Long userId;

//    private Boolean isOnline;

    private Timestamp lastActiveAt;
}
