package com.nexus.cms.chat.config;

import com.nexus.cms.chat.service.interfaces.PresenceService;
import com.nexus.cms.util.StompPrincipalNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

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
            if (userId != null) {
                presenceService.setOnline(userId);
            }
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
            if (userId != null) {
                presenceService.setOffline(userId);
            }
        } catch (Exception e) {
            log.error("Failed to handle SessionDisconnectEvent: {}", e.getMessage(), e);
        }
    }

    private Long extractUserId(StompHeaderAccessor accessor) {
        // Principal name format: "userId:orgId"
        // The session may have no principal — a connection that was rejected,
        // for instance — so this must not blow up.
        Principal user = accessor.getUser();
        if (user == null) {
            return null;
        }
        return StompPrincipalNames.userIdOf(user.getName());
    }
}
