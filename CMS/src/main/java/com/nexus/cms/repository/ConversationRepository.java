package com.nexus.cms.repository;

import com.nexus.cms.model.entities.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /**
     * Find conversation by ID and organization (for multi-tenancy)
     */
    Optional<Conversation> findByIdAndOrgId(Long id, Long orgId);

    /**
     * Find active conversations for organization
     */
    Page<Conversation> findByOrgIdAndIsActive(Long orgId, Boolean isActive, Pageable pageable);

    /**
     * Find direct conversation between two users
     * A DIRECT conversation has exactly 2 participants with given user IDs
     */
    @Query("""
            SELECT c FROM Conversation c
            WHERE c.orgId = :orgId
            AND c.type = com.nexus.cms.model.entities.Conversation.ConversationType.DIRECT
            AND c.isActive = true
            AND (SELECT COUNT(cp) FROM ConversationParticipant cp WHERE cp.conversation = c
                 AND cp.userId IN (:userId1, :userId2)) = 2
            """)
    Optional<Conversation> findDirectConversation(
            @Param("orgId") Long orgId,
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2);

    /**
     * Find conversations where user is participant
     */
    @Query("""
            SELECT DISTINCT c FROM Conversation c
            WHERE c.orgId = :orgId
            AND c.isActive = true
            AND EXISTS (
                SELECT 1 FROM ConversationParticipant cp
                WHERE cp.conversation = c AND cp.userId = :userId
            )
            ORDER BY c.updatedAt DESC
            """)
    Page<Conversation> findUserConversations(
            @Param("orgId") Long orgId,
            @Param("userId") Long userId,
            Pageable pageable);

    /**
     * Find conversations created by specific user
     */
    Page<Conversation> findByOrgIdAndCreatedByAndIsActive(Long orgId, Long createdBy, Boolean isActive,
            Pageable pageable);

    /**
     * Update participant count for conversation
     */
    @Modifying
    @Query("UPDATE Conversation c SET c.participantCount = :count WHERE c.id = :conversationId")
    void updateParticipantCount(@Param("conversationId") Long conversationId, @Param("count") int count);
}
