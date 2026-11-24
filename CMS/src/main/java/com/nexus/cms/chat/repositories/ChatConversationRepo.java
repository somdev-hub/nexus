package com.nexus.cms.chat.repositories;

import com.nexus.cms.chat.entities.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatConversationRepo extends JpaRepository<ChatConversation, Long> {

    @Query("""
                    SELECT cc FROM ChatConversation cc
                    JOIN FETCH cc.chatConversationParticipants
                    WHERE cc.chatConversationId IN (
                        SELECT ccp2.chatConversation.chatConversationId
                        FROM ChatConversationParticipant ccp2
                        WHERE ccp2.participantId = :participantId
                    )
                    ORDER BY cc.lastMessageAt DESC NULLS FIRST, cc.createdAt DESC
            """)
    @Transactional(readOnly = true)
    List<ChatConversation> findByParticipantId(Long participantId);

    @Query("""
                    SELECT cc FROM ChatConversation cc
                    JOIN FETCH cc.chatConversationParticipants ccp
                    WHERE cc.chatConversationId = :conversationId
            """)
    Optional<ChatConversation> findByIdWithParticipants(Long conversationId);

    @Query("""
                    SELECT cc FROM ChatConversation cc
                    JOIN FETCH cc.chatConversationParticipants
                    WHERE cc.chatConversationType = com.nexus.cms.chat.enums.ChatConversationType.DIRECT
                    AND cc.chatConversationId IN (
                        SELECT ccp1.chatConversation.chatConversationId FROM ChatConversationParticipant ccp1
                        WHERE ccp1.participantId = :participantA
                    )
                    AND cc.chatConversationId IN (
                        SELECT ccp2.chatConversation.chatConversationId FROM ChatConversationParticipant ccp2
                        WHERE ccp2.participantId = :participantB
                    )
            """)
    @Transactional(readOnly = true)
    Optional<ChatConversation> findDirectConversationBetween(Long participantA, Long participantB);
}
