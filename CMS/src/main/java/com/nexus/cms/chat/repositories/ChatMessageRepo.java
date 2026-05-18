package com.nexus.cms.chat.repositories;

import com.nexus.cms.chat.entities.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepo extends JpaRepository<ChatMessage, Long> {

    @Query("""
                        SELECT cm FROM ChatMessage cm
                        WHERE cm.chatConversation.chatConversationId = :conversationId
                        AND cm.chatMessageStatus!= 'DELETED_FOR_ME'
                        ORDER BY cm.sentAt DESC
            """)
    List<ChatMessage> findLatestMessages(Long conversationId, Pageable pageable);

    @Query("""
                                    SELECT cm FROM ChatMessage cm
                                    WHERE cm.chatConversation.chatConversationId = :conversationId
                                    AND cm.chatMessageStatus!= 'DELETED_FOR_ME'
                                    AND cm.chatMessageId < :beforeId
                                    ORDER BY cm.sentAt DESC
            """)
    List<ChatMessage> findMessagesBefore(Long conversationId, Pageable pageable, Long beforeId);
}
