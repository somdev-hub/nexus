package com.nexus.cms.chat.service.implementations;

import com.nexus.cms.chat.model.MessageSentEventDto;
import com.nexus.cms.chat.service.interfaces.ChatMessageConsumerService;
import com.nexus.cms.chat.service.interfaces.RedisMessagePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatMessageConsumerServiceImpl implements ChatMessageConsumerService {
    private final RedisMessagePublisher redisMessagePublisher;

    @Override
    public void broadcastMessage(MessageSentEventDto messageSentEventDto) {
        redisMessagePublisher.publishToConversation(messageSentEventDto.getConversationId(), messageSentEventDto);
    }
}
