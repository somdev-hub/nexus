package com.nexus.cms.chat.config;

import com.nexus.cms.chat.service.interfaces.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ChatWebSocketEventListener {

    private final PresenceService presenceService;

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = extractUserId(accessor);
        presenceService.setOnline(userId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = extractUserId(accessor);
        presenceService.setOffline(userId);
    }

    private Long extractUserId(StompHeaderAccessor accessor) {
        return Long.parseLong(Objects.requireNonNull(accessor.getUser()).getName());
    }
}
