package com.nexus.cms.chat.repositories;

import com.nexus.cms.chat.entities.ChatConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatConversationParticipantRepo extends JpaRepository<ChatConversationParticipant, Long> {
}
