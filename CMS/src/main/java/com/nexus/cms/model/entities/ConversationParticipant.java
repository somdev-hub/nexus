package com.nexus.cms.model.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Table(name = "t_conversation_participants", schema = "cms",
       uniqueConstraints = @UniqueConstraint(columnNames = {"conversation_id", "user_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to conversation
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    /**
     * User ID of participant
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * Timestamp when user joined the conversation
     */
    @Column(nullable = false)
    private Timestamp joinedAt;

    /**
     * Last message ID seen by participant (for read status tracking)
     */
    private Long lastSeenMessageId;

    /**
     * Timestamp of last message seen
     */
    private Timestamp lastSeenAt;

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = new Timestamp(System.currentTimeMillis());
        }
    }
}

