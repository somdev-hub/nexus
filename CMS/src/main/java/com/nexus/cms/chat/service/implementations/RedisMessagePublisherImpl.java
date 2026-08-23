package com.nexus.cms.chat.service.implementations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.cms.chat.model.MessageSentEventDto;
import com.nexus.cms.chat.service.interfaces.RedisMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisMessagePublisherImpl implements RedisMessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publishToConversation(Long conversationId, MessageSentEventDto messageSentEventDto) {
        // Publish the DTO object directly so RedisTemplate's Jackson serializer
        // emits a JSON object (START_OBJECT) rather than a JSON string literal.
        try {
            Long receivers = redisTemplate.convertAndSend("conversation:" + conversationId, messageSentEventDto);
            log.info("[CHAT RELAY] Redis publish receivers - conversationId={}, receivers={}", conversationId,
                    receivers);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish message event to Redis", e);
        }
    }
}
