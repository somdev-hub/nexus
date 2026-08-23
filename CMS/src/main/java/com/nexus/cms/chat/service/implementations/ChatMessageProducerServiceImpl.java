package com.nexus.cms.chat.service.implementations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.cms.chat.model.MessageSentEventDto;
import com.nexus.cms.chat.model.NotificationEventDto;
import com.nexus.cms.chat.properties.ChatConstants;
import com.nexus.cms.chat.service.interfaces.ChatMessageProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatMessageProducerServiceImpl implements ChatMessageProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publishMessageSent(MessageSentEventDto messageSentEventDto) {
        try {
            String payload = objectMapper.writeValueAsString(messageSentEventDto);
            kafkaTemplate.send(ChatConstants.MESSAGE_SENT_TOPIC, messageSentEventDto.getConversationId().toString(),
                    payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize chat message sent event", e);
        }
    }

    @Override
    public void publishNotification(NotificationEventDto notificationEventDto) {
        try {
            String payload = objectMapper.writeValueAsString(notificationEventDto);
            kafkaTemplate.send(ChatConstants.NOTIFICATION_TOPIC, notificationEventDto.getUserId().toString(), payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize chat notification event", e);
        }
    }
}
