package com.nexus.cms.model.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "t_conversations", schema = "cms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

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
     * One-to-many relationship with participants
     */
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<ConversationParticipant> participants = new HashSet<>();

    /**
     * One-to-many relationship with messages
     */
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Message> messages = new HashSet<>();

    public enum ConversationType {
        DIRECT,    // One-to-one chat
        GROUP      // Group chat with multiple participants
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

