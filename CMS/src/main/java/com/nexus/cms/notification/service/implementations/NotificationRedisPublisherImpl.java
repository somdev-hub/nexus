package com.nexus.cms.notification.service.implementations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.cms.notification.model.NotificationMessageDto;
import com.nexus.cms.notification.properties.NotificationConstants;
import com.nexus.cms.notification.service.interfaces.NotificationRedisPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationRedisPublisherImpl implements NotificationRedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publishToOrg(Long orgId, NotificationMessageDto message) {
        if (orgId == null || message == null) {
            log.warn("Cannot publish a notification without an organisation or a payload");
            return;
        }
        try {
            // Publish the DTO object directly so RedisTemplate's Jackson
            // serializer emits a JSON object rather than a JSON string
            // literal.
            Long receivers = redisTemplate.convertAndSend(
                    NotificationConstants.REDIS_CHANNEL_PREFIX + orgId, message);
            log.debug("[NOTIFICATION RELAY] Published to Redis — orgId={}, receivers={}", orgId, receivers);
        } catch (Exception e) {
            // The notification is already committed. A relay failure costs the
            // live push, not the notification — the user still finds it in
            // their inbox on the next sync.
            log.error("[NOTIFICATION RELAY] Failed to publish notification for org {} to Redis", orgId, e);
        }
    }
}
