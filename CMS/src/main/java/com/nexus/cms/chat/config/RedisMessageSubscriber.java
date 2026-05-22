package com.nexus.cms.chat.config;

import com.nexus.cms.chat.model.MessageSentEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMessageSubscriber {

    private SimpMessagingTemplate messagingTemplate;
    private ObjectMapper objectMapper;

    // called by RedisMessageListenerContainer for every message on conversation:*
    public void onMessage(String message, String channel) {
        try {
            MessageSentEventDto event = objectMapper.readValue(message, MessageSentEventDto.class);
            long conversationId = Long.parseLong(channel.split(":")[1]);

            // broadcast to WebSocket subscribers on THIS instance
            messagingTemplate.convertAndSend(
                    "/topic/conversations/" + conversationId, event
            );
        } catch (Exception e) {
            log.error("Failed to process Redis message", e);
        }
    }
}
