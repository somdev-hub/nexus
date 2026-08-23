package com.nexus.cms.chat.repositories;

import com.nexus.cms.chat.entities.ChatConversationParticipant;
import com.nexus.cms.chat.entities.ChatMessageIndividualStatus;
import com.nexus.cms.chat.enums.ChatMessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageIndividualStatusRepo extends JpaRepository<ChatMessageIndividualStatus, Long> {

    /**
     * Find all individual statuses for a specific participant that are still in
     * SENT state
     * and belong to the provided message IDs
     */
    @Query("SELECT cis FROM ChatMessageIndividualStatus cis " +
            "WHERE cis.participant.chatConversationParticipantsId = :participantId " +
            "AND cis.status = 'SENT' " +
            "AND cis.chatMessage.chatMessageId IN :messageIds")
    List<ChatMessageIndividualStatus> findSentStatusesByParticipantAndMessages(
            @Param("participantId") Long participantId,
            @Param("messageIds") List<Long> messageIds);

    /**
     * Batch update statuses from SENT to RECEIVED for given individual status
     * records
     */
    @Modifying
    @Transactional
    @Query("UPDATE ChatMessageIndividualStatus cis SET cis.status = :newStatus, cis.receivedAt = CURRENT_TIMESTAMP " +
            "WHERE cis.participant.chatConversationParticipantsId = :participantId " +
            "AND cis.status = 'SENT' " +
            "AND cis.chatMessage.chatMessageId IN :messageIds")
    void batchUpdateStatusToReceived(
            @Param("participantId") Long participantId,
            @Param("messageIds") List<Long> messageIds,
            @Param("newStatus") ChatMessageStatus newStatus);

    @Query("""
                        SELECT cis FROM ChatMessageIndividualStatus cis
                        WHERE cis.chatMessage.chatMessageId = :messageId
                        AND cis.participant = :chatConversationParticipant
            """)
    @Transactional(readOnly = true)
    Optional<ChatMessageIndividualStatus> findByChatMessageIdAndChatConversationParticipant(Long messageId, ChatConversationParticipant chatConversationParticipant);
}
