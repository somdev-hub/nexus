package com.nexus.cms.chat.repositories;

import com.nexus.cms.chat.entities.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface ChatMessageRepo extends JpaRepository<ChatMessage, Long> {

        /**
         * Fetch latest messages for a conversation, ordered by message ID descending.
         * Using chatMessageId instead of sentAt avoids millisecond collision edge
         * cases.
         * Phase 4: Pagination cursor-based using messageId for stability.
         */
        @Query("""
                                    SELECT cm FROM ChatMessage cm
                                    WHERE cm.chatConversation.chatConversationId = :conversationId
                                    AND cm.chatMessageStatus!= 'DELETED_FOR_ME'
                                    AND cm.isActive = true
                                    ORDER BY cm.chatMessageId DESC
                        """)
        List<ChatMessage> findLatestMessages(Long conversationId, Pageable pageable);

        /**
         * Fetch messages before a given message ID (pagination cursor).
         * Using chatMessageId ensures stable pagination: no message can arrive between
         * fetch calls at exact same millisecond.
         * Phase 4: Pagination cursor-based using messageId for stability.
         */
        @Query("""
                                                SELECT cm FROM ChatMessage cm
                                                WHERE cm.chatConversation.chatConversationId = :conversationId
                                                AND cm.chatMessageStatus!= 'DELETED_FOR_ME'
                                                AND cm.chatMessageId < :beforeId
                                                AND cm.isActive = true
                                                ORDER BY cm.chatMessageId DESC
                        """)
        List<ChatMessage> findMessagesBefore(Long conversationId, Pageable pageable, Long beforeId);

        @Query("""
                                                            SELECT cm FROM ChatMessage cm
                                                            WHERE cm.chatConversation.chatConversationId = :conversationId
                                                            AND cm.chatMessageStatus!= 'DELETED_FOR_ME'
                                                            AND cm.sentAt > :messagesAfter
                                                            AND cm.isActive = true
                                                            ORDER BY cm.sentAt ASC
                        """)
        List<ChatMessage> findByChatConversationIdAndAfter(Long conversationId, Timestamp messagesAfter);

        /**
         * Count unread messages for a participant in a conversation.
         * Fixes N+1 problem by using direct database count instead of loading all
         * messages.
         * 
         * @param conversationId The conversation ID
         * @param lastRead       The last read timestamp of the participant
         * @return Count of unread messages
         */
        @Query("""
                                SELECT COUNT(cm) FROM ChatMessage cm
                                WHERE cm.chatConversation.chatConversationId = :conversationId
                                AND cm.sentAt > :lastRead
                                AND cm.chatMessageStatus != 'DELETED_FOR_ME'
                                AND cm.isActive = true
                        """)
        Long countUnreadMessages(Long conversationId, Timestamp lastRead);
}
