package com.nexus.cms.chat.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@RequiredArgsConstructor
public class ChatWebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {
    private final ChatWebSocketAuthInceptor chatWebsocketAuthInceptor;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(chatWebsocketAuthInceptor);
    }
}
