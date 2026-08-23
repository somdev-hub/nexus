package com.nexus.cms.chat.repositories;

import com.nexus.cms.chat.entities.ChatConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Optional;

@Repository
public interface ChatConversationParticipantRepo extends JpaRepository<ChatConversationParticipant, Long> {

    /**
     * Update lastRead and lastMessageId for a participant
     * Only advances lastRead if the new timestamp is newer
     */
    @Modifying
    @Transactional
    @Query("UPDATE ChatConversationParticipant ccp SET " +
            "ccp.lastRead = CASE WHEN ccp.lastRead IS NULL OR :lastRead > ccp.lastRead THEN :lastRead ELSE ccp.lastRead END, "
            +
            "ccp.lastMessageId = :lastMessageId " +
            "WHERE ccp.chatConversationParticipantsId = :participantId")
    void updateLastReadAndMessageId(
            @Param("participantId") Long participantId,
            @Param("lastRead") Timestamp lastRead,
            @Param("lastMessageId") Long lastMessageId);

    @Query("""
                        SELECT ccp FROM ChatConversationParticipant ccp
                        WHERE ccp.chatConversationParticipantsId = :chatConversationId
                        AND ccp.participantId = :participantId
            """)
    @Transactional(readOnly = true)
    Optional<ChatConversationParticipant> findByChatConversationIdAndParticipantId(Long chatConversationId, Long participantId);
}
