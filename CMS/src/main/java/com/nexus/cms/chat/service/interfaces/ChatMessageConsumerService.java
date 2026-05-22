package com.nexus.cms.chat.service.interfaces;

import com.nexus.cms.chat.model.MessageSentEventDto;
import com.nexus.cms.chat.properties.ChatConstants;
import org.springframework.kafka.annotation.KafkaListener;

public interface ChatMessageConsumerService {

    @KafkaListener(topics = ChatConstants.MESSAGE_SENT_TOPIC, groupId = "chat-websocket-group")
    void broadcastMessage(MessageSentEventDto messageSentEventDto);
}
