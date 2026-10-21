package com.nexus.cms.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.cms.notification.model.NotificationMessageDto;
import com.nexus.cms.notification.properties.NotificationConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Receives a notification from Redis and pushes it to the clients connected
 * to this instance.
 * <p>
 * Only the notification channel pattern is handled. Anything else on Redis is
 * left alone, so the chat relay is unaffected.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message redisMessage, byte[] pattern) {
        String channel = redisMessage.getChannel() != null
                ? new String(redisMessage.getChannel(), StandardCharsets.UTF_8)
                : null;
        String body = redisMessage.getBody() != null
                ? new String(redisMessage.getBody(), StandardCharsets.UTF_8)
                : null;

        if (channel == null || body == null || body.isBlank()) {
            return;
        }

        try {
            Long orgId = organisationIdOf(channel);
            if (orgId == null) {
                log.debug("Ignoring Redis message on an unrecognised channel: {}", channel);
                return;
            }

            NotificationMessageDto notification = objectMapper.readValue(body, NotificationMessageDto.class);
            String destination = NotificationConstants.ORG_TOPIC_PREFIX + orgId;
            messagingTemplate.convertAndSend(destination, notification);
            log.debug("[NOTIFICATION RELAY] Relayed to STOMP — destination={}, notificationId={}",
                    destination, notification.getNotificationId());
        } catch (Exception e) {
            log.error("[NOTIFICATION RELAY] Failed to relay a notification from channel {}", channel, e);
        }
    }

    /**
     * The organisation id carried by a Redis channel, or null when the
     * channel is not one of ours.
     */
    private Long organisationIdOf(String channel) {
        if (!channel.startsWith(NotificationConstants.REDIS_CHANNEL_PREFIX)) {
            return null;
        }
        String orgId = channel.substring(NotificationConstants.REDIS_CHANNEL_PREFIX.length());
        try {
            return Long.valueOf(orgId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
