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

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    // called by RedisMessageListenerContainer for every message on conversation:*
    public void onMessage(String message, String channel) {
        try {
            MessageSentEventDto event = objectMapper.readValue(message, MessageSentEventDto.class);
            long conversationId = extractConversationId(channel);

            if (conversationId <= 0) {
                log.error("Invalid channel format: {}", channel);
                return;
            }

            // broadcast to WebSocket subscribers on THIS instance
            messagingTemplate.convertAndSend(
                    "/topic/conversations/" + conversationId, event);
        } catch (NumberFormatException e) {
            log.error("Invalid conversation ID in channel {}: {}", channel, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to process Redis message from channel {}: {}",
                    channel, e.getClass().getSimpleName(), e);
            logToDeadLetter(channel, message, e);
        }
    }

    private long extractConversationId(String channel) throws NumberFormatException {
        String[] parts = channel.split(":");
        if (parts.length < 2) {
            throw new NumberFormatException("Invalid channel format: " + channel);
        }
        return Long.parseLong(parts[1]);
    }

    /**
     * Log failed messages to dead letter queue for potential manual recovery.
     * In production, consider using a dedicated DLQ topic or logging service.
     */
    private void logToDeadLetter(String channel, String message, Exception e) {
        log.warn("Message sent to dead letter - Channel: {}, Error: {}, Message: {}",
                channel, e.getClass().getSimpleName(), message);
        // TODO: Implement proper dead letter queue (database table, separate Kafka
        // topic, etc.)
    }
}
