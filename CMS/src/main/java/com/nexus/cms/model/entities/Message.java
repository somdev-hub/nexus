package com.nexus.cms.model.entities;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "t_messages", schema = "cms", indexes = {
        @Index(name = "idx_conversation_id", columnList = "conversation_id"),
        @Index(name = "idx_sender_id", columnList = "sender_id"),
        @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    /**
     * Unique message ID (UUID for idempotency and distributed generation)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    /**
     * Reference to conversation
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    /**
     * ID of conversation for denormalization (improves query performance)
     */
    @Column(name = "conversation_id", insertable = false, updatable = false, nullable = false)
    private Long conversationId;

    /**
     * User ID of message sender
     */
    @Column(nullable = false)
    private Long senderId;

    /**
     * Message content
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Message creation timestamp
     */
    @Column(nullable = false)
    private Timestamp timestamp;

    /**
     * Message status: SENT (published to Kafka), DELIVERED (persisted to DB)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;

    /**
     * Organization ID for multi-tenancy
     */
    @Column(nullable = false)
    private Long orgId;

    /**
     * Optional: message type for future extensibility (TEXT, IMAGE, FILE, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MessageType type = MessageType.TEXT;

    public enum MessageStatus {
        SENT, // Published to Kafka topic
        DELIVERED // Persisted to database
    }

    public enum MessageType {
        TEXT, // Plain text message
        IMAGE, // Image attachment (future)
        FILE, // File attachment (future)
        SYSTEM // System message (user joined, etc.)
    }

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = new Timestamp(System.currentTimeMillis());
        }
    }
}
