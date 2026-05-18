package com.nexus.cms.chat.repositories;

import com.nexus.cms.chat.entities.ChatMessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageAttachmentRepo extends JpaRepository<ChatMessageAttachment, Long> {
}
