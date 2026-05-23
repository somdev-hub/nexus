package com.nexus.cms.chat.service.implementations;

import com.nexus.cms.chat.model.MessageSentEventDto;
import com.nexus.cms.chat.properties.ChatConstants;
import com.nexus.cms.chat.service.interfaces.ChatMessageConsumerService;
import com.nexus.cms.chat.service.interfaces.RedisMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageConsumerServiceImpl implements ChatMessageConsumerService {
    private final RedisMessagePublisher redisMessagePublisher;

    @Override
    @KafkaListener(topics = ChatConstants.MESSAGE_SENT_TOPIC, groupId = "chat-websocket-group", containerFactory = "kafkaListenerContainerFactory")
    public void broadcastMessage(MessageSentEventDto messageSentEventDto) {
        try {
            log.debug("Received message from Kafka - ConversationId: {}, MessageId: {}",
                    messageSentEventDto.getConversationId(),
                    messageSentEventDto.getMessageId());

            if (messageSentEventDto.getConversationId() == null) {
                log.error("Received message with null conversationId");
                return;
            }

            redisMessagePublisher.publishToConversation(
                    messageSentEventDto.getConversationId(),
                    messageSentEventDto);

            log.debug("Successfully published message to Redis - ConversationId: {}",
                    messageSentEventDto.getConversationId());
        } catch (Exception e) {
            log.error("Failed to broadcast message from Kafka", e);
            // Kafka will retry based on configuration
        }
    }
}
