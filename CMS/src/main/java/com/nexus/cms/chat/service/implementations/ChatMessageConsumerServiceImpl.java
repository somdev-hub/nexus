package com.nexus.cms.chat.service.implementations;

import com.nexus.cms.chat.model.MessageSentEventDto;
import com.nexus.cms.chat.properties.ChatConstants;
import com.nexus.cms.chat.service.interfaces.ChatMessageConsumerService;
import com.nexus.cms.chat.service.interfaces.RedisMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageConsumerServiceImpl implements ChatMessageConsumerService {
    private final RedisMessagePublisher redisMessagePublisher;
    private final ObjectMapper objectMapper;

    @Override
    @KafkaListener(topics = ChatConstants.MESSAGE_SENT_TOPIC, groupId = "chat-websocket-group", containerFactory = "singleRecordKafkaListenerContainerFactory")
    public void broadcastMessage(String message) {
        try {
            log.info("[CHAT RELAY] Received message from Kafka (raw JSON): {}",
                    message.length() > 500 ? message.substring(0, 500) : message);

            MessageSentEventDto messageSentEventDto = objectMapper.readValue(message, MessageSentEventDto.class);

            log.info("[CHAT RELAY] Kafka parsed - conversationId={}, messageId={}",
                    messageSentEventDto.getConversationId(),
                    messageSentEventDto.getMessageId());

            if (messageSentEventDto.getConversationId() == null) {
                log.error("Received message with null conversationId");
                return;
            }

            redisMessagePublisher.publishToConversation(
                    messageSentEventDto.getConversationId(),
                    messageSentEventDto);

            log.info("[CHAT RELAY] Published to Redis - conversationId={}",
                    messageSentEventDto.getConversationId());
        } catch (Exception e) {
            log.error("[CHAT RELAY] Failed to relay Kafka message", e);
            // Kafka will retry based on configuration
        }
    }
}
