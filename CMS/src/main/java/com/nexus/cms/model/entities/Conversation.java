package com.nexus.cms.model.entities;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "t_conversations", schema = "cms", indexes = {
        @Index(name = "idx_org_id_active", columnList = "org_id, is_active"),
        @Index(name = "idx_created_by", columnList = "created_by")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Conversation name - optional for DIRECT chats, required for GROUP chats
     */
    private String name;

    /**
     * Type: DIRECT (1-1) or GROUP
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationType type;

    /**
     * User ID of conversation creator
     */
    @Column(nullable = false)
    private Long createdBy;

    /**
     * Organization ID for multi-tenancy
     */
    @Column(nullable = false)
    private Long orgId;

    /**
     * Conversation creation timestamp
     */
    @Column(nullable = false)
    private Timestamp createdAt;

    /**
     * Last update timestamp
     */
    private Timestamp updatedAt;

    /**
     * Active status
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Participant count (denormalized for better query performance)
     */
    @Column(name = "participant_count", nullable = false)
    @Builder.Default
    private Integer participantCount = 0;

    /**
     * Last message ID for quick access to latest message
     */
    private Long lastMessageId;

    /**
     * Last message timestamp
     */
    private Timestamp lastMessageAt;

    public enum ConversationType {
        DIRECT, // One-to-one chat
        GROUP // Group chat with multiple participants
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = new Timestamp(System.currentTimeMillis());
        }
        if (updatedAt == null) {
            updatedAt = new Timestamp(System.currentTimeMillis());
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Timestamp(System.currentTimeMillis());
    }
}
