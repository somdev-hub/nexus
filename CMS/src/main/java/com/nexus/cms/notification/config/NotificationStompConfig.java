package com.nexus.cms.notification.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Registers the notification subscription guard on the client inbound
 * channel.
 * <p>
 * <b>This configurer must stay minimal.</b> It overrides
 * {@code configureClientInboundChannel} and nothing else, for two reasons:
 * <ul>
 *   <li>calling {@code enableSimpleBroker()} again would <em>replace</em> the
 *   broker that {@code ChatWebSocketConfig} registered and break every
 *   subscription in the application;</li>
 *   <li>calling {@code registerStompEndpoints()} again would register a
 *   second endpoint, which is exactly what broke the chat subsystem once
 *   before. See {@code maintainance-docs/WEBSOCKET_DEBUGGING_GUIDE.md}.</li>
 * </ul>
 * The notification subsystem therefore shares the chat subsystem's broker and
 * its {@code /ws} endpoint, and is kept apart by its own destination
 * namespace.
 * <p>
 * The order matters. The chat authentication interceptor establishes the
 * session principal, and the guard relies on it, so the chat configurer runs
 * first.
 */
@Configuration
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class NotificationStompConfig implements WebSocketMessageBrokerConfigurer {

    private final NotificationSubscriptionGuard notificationSubscriptionGuard;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(notificationSubscriptionGuard);
    }
}
