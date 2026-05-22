package com.nexus.cms.chat.service.implementations;

import com.nexus.cms.chat.entities.ChatParticipantsPresence;
import com.nexus.cms.chat.model.PresenceEventDto;
import com.nexus.cms.chat.properties.ChatConstants;
import com.nexus.cms.chat.repositories.ChatParticipantsPresenceRepo;
import com.nexus.cms.chat.service.interfaces.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PresenceServiceImpl implements PresenceService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatParticipantsPresenceRepo chatParticipantsPresenceRepo;

    @Override
    public void setOnline(Long userId) {
        redisTemplate.opsForValue().set(
                ChatConstants.PRESENCE_KEY + userId,
                PresenceEventDto.PresenceEventStatus.ONLINE,
                ChatConstants.PRESENCE_TTL_MINUTES, TimeUnit.MINUTES  // auto-expires if server crashes
        );
        broadcast(userId, PresenceEventDto.PresenceEventStatus.ONLINE);
    }

    @Override
    public void setOffline(Long userId) {
        redisTemplate.delete(ChatConstants.PRESENCE_KEY + userId);
        broadcast(userId, PresenceEventDto.PresenceEventStatus.OFFLINE);
        if (chatParticipantsPresenceRepo.existsByUserId(userId)) {
            chatParticipantsPresenceRepo.updateLastActive(userId, new Timestamp(System.currentTimeMillis()));
        } else {
            ChatParticipantsPresence chatParticipantsPresence = new ChatParticipantsPresence();
            chatParticipantsPresence.setUserId(userId);
            chatParticipantsPresence.setLastActiveAt(new Timestamp(System.currentTimeMillis()));
            chatParticipantsPresenceRepo.save(chatParticipantsPresence);
        }
    }

    @Override
    public boolean isOnline(Long userId) {
        return redisTemplate.hasKey(ChatConstants.PRESENCE_KEY + userId);
    }

    @Override
    public void heartbeat(Long userId) {
        redisTemplate.expire(
                ChatConstants.PRESENCE_KEY + userId,
                ChatConstants.PRESENCE_TTL_MINUTES, TimeUnit.MINUTES
        );
    }

    @Override
    public void broadcast(Long userId, PresenceEventDto.PresenceEventStatus status) {
        messagingTemplate.convertAndSend("/topic/presence", new PresenceEventDto(userId, status, new Timestamp(System.currentTimeMillis())));
    }
}
