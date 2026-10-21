package com.nexus.cms.chat.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Registers the chat authentication interceptor on the client inbound channel.
 * <p>
 * <b>Ordering matters.</b> The inbound channel is shared by every STOMP
 * endpoint in this application, so the interceptors registered here run for
 * chat and notification frames alike. The authentication interceptor must
 * therefore run first: it validates the CONNECT frame and establishes the
 * session principal, which the notification subscription guard then relies on.
 * <p>
 * This configurer deliberately overrides <b>only</b> {@code configureClientInboundChannel}.
 * It must never call {@code enableSimpleBroker()} — a second call replaces the
 * broker that {@code ChatWebSocketConfig} registered and breaks every
 * subscription. It must also never call {@code registerStompEndpoints()}, for
 * the same reason. See
 * {@code maintainance-docs/WEBSOCKET_DEBUGGING_GUIDE.md}.
 */
@Configuration
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ChatWebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {
    private final ChatWebSocketAuthInceptor chatWebsocketAuthInceptor;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(chatWebsocketAuthInceptor);
    }
}
