package com.nexus.cms.chat.service.implementations;

import com.nexus.cms.chat.model.TypingEventDto;
import com.nexus.cms.chat.properties.ChatConstants;
import com.nexus.cms.chat.service.interfaces.TypingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TypingServiceImpl implements TypingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void setTyping(Long conversationId, Long participantId, boolean isTyping) {
        String key = String.format(ChatConstants.TYPING_KEY, conversationId, participantId);

        if (isTyping) {
            redisTemplate.opsForValue().set(key, "1",
                    ChatConstants.TYPING_TTL_SECONDS, TimeUnit.SECONDS);  // auto-clears after 5s
        } else {
            redisTemplate.delete(key);
        }

        messagingTemplate.convertAndSend(
                "/topic/conversations/" + conversationId + "/typing",
                new TypingEventDto(participantId, isTyping)
        );
    }

    @Override
    public List<Long> getTypingUsers(Long conversationId) {
        // get all users currently typing in this conversation using SCAN (non-blocking)
        List<Long> users = new ArrayList<>();
        org.springframework.data.redis.core.ScanOptions options =
                org.springframework.data.redis.core.ScanOptions.scanOptions()
                        .match("typing:" + conversationId + ":*").count(100).build();
        try (org.springframework.data.redis.core.Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                String[] parts = key.split(":");
                if (parts.length >= 3) {
                    try {
                        users.add(Long.parseLong(parts[2]));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to scan typing keys for conversation {}", conversationId, e);
        }
        return users;
    }
}
