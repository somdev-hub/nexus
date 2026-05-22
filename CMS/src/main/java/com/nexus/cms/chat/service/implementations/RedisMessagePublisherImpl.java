package com.nexus.cms.chat.service.implementations;

import com.nexus.cms.chat.model.MessageSentEventDto;
import com.nexus.cms.chat.service.interfaces.RedisMessagePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisMessagePublisherImpl implements RedisMessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void publishToConversation(Long conversationId, MessageSentEventDto messageSentEventDto) {
        redisTemplate.convertAndSend("conversation:" + conversationId, messageSentEventDto);
    }
}
