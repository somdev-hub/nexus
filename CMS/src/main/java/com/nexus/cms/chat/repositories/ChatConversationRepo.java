package com.nexus.cms.chat.repositories;

import com.nexus.cms.chat.entities.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ChatConversationRepo extends JpaRepository<ChatConversation, Long> {

    @Query("""
                    SELECT cc FROM ChatConversation cc
                    JOIN cc.chatConversationParticipants ccp
                    WHERE ccp.participantId = :participantId
                    ORDER BY cc.lastMessageAt DESC
            """)
    @Transactional(readOnly = true)
    List<ChatConversation> findByParticipantId(Long participantId);
}
