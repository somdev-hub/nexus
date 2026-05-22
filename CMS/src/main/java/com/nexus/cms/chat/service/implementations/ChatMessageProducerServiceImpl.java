package com.nexus.cms.chat.service.implementations;

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

    @Override
    public void publishMessageSent(MessageSentEventDto messageSentEventDto) {
        kafkaTemplate.send(ChatConstants.MESSAGE_SENT_TOPIC, messageSentEventDto.getConversationId().toString(), messageSentEventDto);
    }

    @Override
    public void publishNotification(NotificationEventDto notificationEventDto) {
        kafkaTemplate.send(ChatConstants.NOTIFICATION_TOPIC, notificationEventDto.getUserId().toString(), notificationEventDto);
    }
}
