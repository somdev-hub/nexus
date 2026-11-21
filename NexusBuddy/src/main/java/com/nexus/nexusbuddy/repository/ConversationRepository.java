package com.nexus.nexusbuddy.repository;

import com.nexus.nexusbuddy.model.entities.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(String userId);
    Optional<Conversation> findByIdAndUserId(Long id, String userId);
    void deleteByIdAndUserId(Long id, String userId);
}