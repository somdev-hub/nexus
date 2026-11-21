package com.nexus.nexusbuddy.model.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

/**
 * Entity to store chat conversation history for NexusBuddy.
 * Messages are stored as a JSON string for simplicity.
 */
@Entity
@Table(name = "t_conversation", schema = "nexusbuddy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conversation_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "org_id")
    private Long orgId;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "context", length = 255)
    private String context;

    @Column(name = "messages_json", columnDefinition = "TEXT")
    private String messagesJson;

    @Column(name = "backend_conversation_id", length = 255)
    private String backendConversationId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "is_active")
    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = true;
        }
    }
}