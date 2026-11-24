package com.nexus.cms.chat.repositories;

import com.nexus.cms.chat.entities.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepo extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByChatConversation_ChatConversationIdAndClientMessageId(Long conversationId, String clientMessageId);

        /**
         * Fetch latest messages for a conversation, ordered by message ID descending.
         * Using chatMessageId instead of sentAt avoids millisecond collision edge
         * cases.
         * Phase 4: Pagination cursor-based using messageId for stability.
         * Eagerly loads chatConversationParticipant to avoid N+1 and NPE issues.
         */
        @Query("""
                                    SELECT cm FROM ChatMessage cm
                                    LEFT JOIN FETCH cm.chatConversationParticipant ccp
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
         * Eagerly loads chatConversationParticipant to avoid N+1 and NPE issues.
         */
        @Query("""
                                                SELECT cm FROM ChatMessage cm
                                                LEFT JOIN FETCH cm.chatConversationParticipant ccp
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
                                AND cm.chatMessageId > :lastReadMessageId
                                AND (cm.chatConversationParticipant IS NULL OR cm.chatConversationParticipant.participantId <> :participantId)
                                AND cm.chatMessageStatus != 'DELETED_FOR_ME'
                                AND cm.isActive = true
                        """)
        Long countUnreadMessagesByCursor(Long conversationId, Long lastReadMessageId, Long participantId);

        @Query("""
                        SELECT cm FROM ChatMessage cm
                        LEFT JOIN FETCH cm.chatConversationParticipant ccp
                        WHERE cm.chatConversation.chatConversationId = :conversationId
                        AND (cm.isActive IS NULL OR cm.isActive = true)
                        ORDER BY cm.chatMessageId DESC
                """)
        List<ChatMessage> findAllActiveByConversation(Long conversationId);

        @Query("""
                        SELECT cm FROM ChatMessage cm
                        LEFT JOIN FETCH cm.chatConversationParticipant ccp
                        WHERE cm.chatConversation.chatConversationId = :conversationId
                        ORDER BY cm.sentAt ASC
                """)
        List<ChatMessage> findAllByConversationOrderBySentAtAsc(Long conversationId);

        @Query("""
                        SELECT cm FROM ChatMessage cm
                        WHERE cm.chatConversation.chatConversationId = :conversationId
                        AND cm.chatMessageText ILIKE %:text%
                        AND (cm.isActive IS NULL OR cm.isActive = true)
                """)
        List<ChatMessage> findByConversationIdAndTextContaining(Long conversationId, String text);
}
