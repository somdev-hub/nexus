package com.nexus.cms.chat.config;

import com.nexus.cms.chat.model.MessageSentEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMessageSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    // Backwards-compatible helper signature (can be used by manual calls/tests)
    public void onMessage(String message, String channel) {
        try {
            processPayload(channel, message);
        } catch (Exception e) {
            log.error("Failed to process Redis message from channel {}: {}", channel, e.getClass().getSimpleName(), e);
            logToDeadLetter(channel, message, e);
        }
    }

    @Override
    public void onMessage(Message redisMessage, byte[] pattern) {
        String channel = null;
        String body = null;
        try {
            channel = redisMessage.getChannel() != null ? new String(redisMessage.getChannel(), StandardCharsets.UTF_8)
                    : null;
            body = redisMessage.getBody() != null ? new String(redisMessage.getBody(), StandardCharsets.UTF_8) : null;

            log.info("[CHAT RELAY] Redis pub/sub raw message on {}: {}", channel,
                    body != null && body.length() > 500 ? body.substring(0, 500) : body);

            if (body == null) {
                log.warn("Received empty Redis message on channel {}", channel);
                return;
            }

            // Some publishers may double-serialize JSON into a quoted string ("{...}").
            String payload = body;
            String trimmed = payload.trim();
            if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                // Unwrap JSON string literal first
                payload = objectMapper.readValue(payload, String.class);
            }

            processPayload(channel, payload);
        } catch (NumberFormatException e) {
            log.error("Invalid conversation ID in channel {}: {}", channel, e.getMessage());
            logToDeadLetter(channel != null ? channel : "unknown", body != null ? body : "", e);
        } catch (Exception e) {
            log.error("Failed to process Redis message from channel {}: {}",
                    channel, e.getClass().getSimpleName(), e);
            logToDeadLetter(channel != null ? channel : "unknown", body != null ? body : "", e);
        }
    }

    private void processPayload(String channel, String payload) throws Exception {
        if (payload == null) {
            log.warn("Received empty payload for channel {}", channel);
            return;
        }

        String trimmed = payload.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            payload = objectMapper.readValue(payload, String.class);
        }

        MessageSentEventDto event = objectMapper.readValue(payload, MessageSentEventDto.class);
        long conversationId = extractConversationId(channel);

        if (conversationId <= 0) {
            log.debug("Ignoring non-conversation channel: {}", channel);
            return;
        }

        log.info("[CHAT RELAY] Broadcasting to STOMP - conversationId={}, messageId={}",
                conversationId, event.getMessageId());
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, event);
    }

    private long extractConversationId(String channel) throws NumberFormatException {
        if (channel == null) {
            throw new NumberFormatException("Invalid channel: null");
        }

        // channel may sometimes be a subscription pattern (e.g. "conversation:*")
        // or another non-numeric channel. Extract trailing numeric id if present.
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("conversation:(\\d+)$").matcher(channel);
        if (!m.find()) {
            // Not a concrete conversation channel
            return -1L;
        }
        return Long.parseLong(m.group(1));
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
