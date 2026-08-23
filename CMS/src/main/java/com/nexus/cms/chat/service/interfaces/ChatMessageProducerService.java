package com.nexus.cms.chat.service.interfaces;

import com.nexus.cms.chat.model.MessageSentEventDto;
import com.nexus.cms.chat.model.NotificationEventDto;

public interface ChatMessageProducerService {

    void publishMessageSent(MessageSentEventDto messageSentEventDto);

    void publishNotification(NotificationEventDto notificationEventDto);

}
