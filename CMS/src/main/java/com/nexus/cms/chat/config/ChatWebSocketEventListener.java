package com.nexus.cms.chat.config;

import com.nexus.cms.chat.service.interfaces.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketEventListener {

    private final PresenceService presenceService;

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        try {
            Long userId = extractUserId(accessor);
            log.debug("WebSocket connected - principal={}, userId={}", accessor.getUser(), userId);
            presenceService.setOnline(userId);
        } catch (Exception e) {
            log.error("Failed to handle SessionConnectedEvent: {}", e.getMessage(), e);
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        try {
            Long userId = extractUserId(accessor);
            log.debug("WebSocket disconnected - principal={}, userId={}", accessor.getUser(), userId);
            presenceService.setOffline(userId);
        } catch (Exception e) {
            log.error("Failed to handle SessionDisconnectEvent: {}", e.getMessage(), e);
        }
    }

    private Long extractUserId(StompHeaderAccessor accessor) {
        // Principal name format: "userId:orgId"
        String principalName = Objects.requireNonNull(accessor.getUser()).getName();
        String[] parts = principalName.contains(":") ? principalName.split(":") : new String[] { principalName };
        return Long.parseLong(parts[0]);
    }
}
