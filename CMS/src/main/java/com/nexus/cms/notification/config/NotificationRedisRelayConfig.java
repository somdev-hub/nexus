package com.nexus.cms.notification.config;

import com.nexus.cms.notification.properties.NotificationConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * The Redis relay that carries a notification from the instance which
 * consumed the event to the instance the browser is connected to.
 * <p>
 * The STOMP broker is in-memory and lives inside a single JVM, so a
 * notification produced on one instance is invisible to clients connected to
 * another. Publishing to Redis and relaying on every instance closes that
 * gap.
 * <p>
 * This container is <b>separate</b> from the one the chat subsystem uses: it
 * listens on its own channel pattern ({@code notification:*}) and has its own
 * container, so the two relays cannot interfere with each other.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class NotificationRedisRelayConfig {

    private final NotificationRedisSubscriber notificationRedisSubscriber;

    @Bean
    public RedisMessageListenerContainer notificationRedisListenerContainer(
            RedisConnectionFactory connectionFactory) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(notificationRedisSubscriber,
                new PatternTopic(NotificationConstants.REDIS_CHANNEL_PATTERN));
        log.info("Notification Redis relay listening on pattern {}",
                NotificationConstants.REDIS_CHANNEL_PATTERN);
        return container;
    }
}
