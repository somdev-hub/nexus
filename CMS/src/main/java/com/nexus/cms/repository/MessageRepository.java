package com.nexus.cms.repository;

import com.nexus.cms.model.entities.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * Find message by ID with organization check (multi-tenancy)
     */
    Optional<Message> findByIdAndOrgId(UUID id, Long orgId);

    /**
     * Find messages in conversation ordered by timestamp (for pagination)
     */
    Page<Message> findByConversationIdOrderByTimestampDesc(UUID conversationId, Pageable pageable);

    /**
     * Find messages in conversation after specific timestamp
     */
    List<Message> findByConversationIdAndTimestampAfterOrderByTimestamp(
            UUID conversationId,
            Timestamp timestamp
    );

    /**
     * Count total messages in conversation
     */
    long countByConversationId(UUID conversationId);

    /**
     * Find messages by sender in specific conversation
     */
    @Query("""
            SELECT m FROM Message m 
            WHERE m.conversationId = :conversationId 
            AND m.senderId = :senderId
            ORDER BY m.timestamp DESC
            """)
    Page<Message> findBySenderInConversation(
            @Param("conversationId") UUID conversationId,
            @Param("senderId") Long senderId,
            Pageable pageable
    );

    /**
     * Find all messages sent by user in organization
     */
    @Query("""
            SELECT m FROM Message m 
            WHERE m.senderId = :senderId 
            AND m.orgId = :orgId
            ORDER BY m.timestamp DESC
            """)
    Page<Message> findUserMessages(
            @Param("senderId") Long senderId,
            @Param("orgId") Long orgId,
            Pageable pageable
    );

    /**
     * Count messages delivered in conversation
     */
    @Query("""
            SELECT COUNT(m) FROM Message m 
            WHERE m.conversationId = :conversationId 
            AND m.status = com.nexus.cms.model.entities.Message.MessageStatus.DELIVERED
            """)
    long countDeliveredMessages(@Param("conversationId") UUID conversationId);

    /**
     * Find messages between two timestamps for pagination recovery
     */
    @Query("""
            SELECT m FROM Message m 
            WHERE m.conversationId = :conversationId 
            AND m.timestamp BETWEEN :startTime AND :endTime
            ORDER BY m.timestamp DESC
            """)
    List<Message> findMessagesBetweenTimestamps(
            @Param("conversationId") UUID conversationId,
            @Param("startTime") Timestamp startTime,
            @Param("endTime") Timestamp endTime
    );

    /**
     * Find latest message in conversation
     */
    @Query("""
            SELECT m FROM Message m 
            WHERE m.conversationId = :conversationId
            ORDER BY m.timestamp DESC
            LIMIT 1
            """)
    Optional<Message> findLatestMessage(@Param("conversationId") UUID conversationId);

    /**
     * Check if message exists for idempotency
     */
    boolean existsById(UUID messageId);
}

