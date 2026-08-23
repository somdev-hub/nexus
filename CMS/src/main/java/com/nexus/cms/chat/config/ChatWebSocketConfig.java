package com.nexus.cms.chat.config;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class ChatWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
        WebSocketMessageBrokerConfigurer.super.configureMessageBroker(registry);

        // prefix for messages FROM server TO client (subscriptions)
        registry.enableSimpleBroker("/topic", "/queue");

        // prefix for messages FROM client TO server
        registry.setApplicationDestinationPrefixes("/app");

        // prefix for user-specific messages (for private delivery)
        registry.setUserDestinationPrefix("/user");

    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        WebSocketMessageBrokerConfigurer.super.registerStompEndpoints(registry);

        registry.addEndpoint("/ws")           // client connects here
                .setAllowedOriginPatterns("*")
                .withSockJS();                // fallback for browsers that don't support WS

    }
}
