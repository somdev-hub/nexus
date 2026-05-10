package com.nexus.cms.repository;

import com.nexus.cms.model.entities.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {

    /**
     * Find participant by conversation ID and user ID
     */
    Optional<ConversationParticipant> findByConversationIdAndUserId(UUID conversationId, Long userId);

    /**
     * Find all participants in a conversation
     */
    List<ConversationParticipant> findByConversationId(UUID conversationId);

    /**
     * Check if user is participant of conversation
     */
    boolean existsByConversationIdAndUserId(UUID conversationId, Long userId);

    /**
     * Count participants in conversation
     */
    long countByConversationId(UUID conversationId);

    /**
     * Find all conversations for a user
     */
    @Query("""
            SELECT cp FROM ConversationParticipant cp 
            WHERE cp.userId = :userId
            ORDER BY cp.joinedAt DESC
            """)
    List<ConversationParticipant> findAllByUserId(@Param("userId") Long userId);

    /**
     * Delete participant from conversation
     */
    void deleteByConversationIdAndUserId(UUID conversationId, Long userId);

    /**
     * Check if user is in multiple conversations
     */
    @Query("""
            SELECT COUNT(cp) FROM ConversationParticipant cp
            WHERE cp.userId = :userId
            AND cp.conversation IN (
                SELECT c FROM Conversation c WHERE c.orgId = :orgId
            )
            """)
    long countUserConversationsInOrg(@Param("userId") Long userId, @Param("orgId") Long orgId);
}

