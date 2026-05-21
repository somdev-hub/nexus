package com.nexus.cms.chat.repositories;

import com.nexus.cms.chat.entities.ChatParticipantsPresence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatParticipantsPresenceRepo extends JpaRepository<ChatParticipantsPresence, Long> {
    boolean existsByUserId(Long userId);

    @Modifying
    @Query("""
            UPDATE ChatParticipantsPresence cpp
            SET cpp.isOnline = :isOnline
            WHERE cpp.userId = :userId
            """)
    void updatePresenceStatus(Long userId, boolean isOnline);
}
