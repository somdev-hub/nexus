package com.nexus.cms.chat.service.interfaces;

import com.nexus.cms.chat.model.MessageSentEventDto;

public interface RedisMessagePublisher {
    public void publishToConversation(Long conversationId, MessageSentEventDto messageSentEventDto);
}
