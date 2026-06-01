package com.nexus.cms.chat.config;

import com.nexus.cms.chat.service.interfaces.PresenceService;
import com.nexus.cms.payload.TokenPayloadDto;
import com.nexus.cms.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketAuthInceptor implements ChannelInterceptor {
    private final CommonUtils commonUtils;
    private final ObjectProvider<PresenceService> presenceServiceProvider;

    @Override
    public @Nullable Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor
                .getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                // client sends JWT in the CONNECT frame header
                String token = accessor.getFirstNativeHeader("Authorization");

                if (token != null) {
                    log.debug("WebSocket CONNECT received Authorization header (masked)");
                    if (token.startsWith("Bearer ")) {
                        token = token.substring(7);
                    }
                    boolean valid = commonUtils.validateToken(token);
                    log.debug("WebSocket token validation result: {}", valid);
                    if (valid) {
                        TokenPayloadDto tokenPayloadDto = commonUtils.decryptToken(token);
                        if (tokenPayloadDto != null) {
                            log.debug("WebSocket token decrypted: userId={}, orgId={}", tokenPayloadDto.getUserId(),
                                    tokenPayloadDto.getOrgId());
                            StompPrincipal principal = new StompPrincipal(String.valueOf(tokenPayloadDto.getUserId()),
                                    String.valueOf(tokenPayloadDto.getOrgId()));
                            accessor.setUser(principal);

                            PresenceService presenceService = presenceServiceProvider.getIfAvailable();
                            if (presenceService != null) {
                                log.debug("WebSocket CONNECT marking user {} online", tokenPayloadDto.getUserId());
                                presenceService.setOnline(tokenPayloadDto.getUserId());
                            }
                        } else {
                            log.warn("WebSocket token decryption returned null payload");
                        }
                    }
                } else {
                    throw new MessagingException("Missing or invalid Authorization header in WebSocket CONNECT frame");
                }
            } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                if (accessor.getUser() != null) {
                    String principalName = accessor.getUser().getName();
                    String[] parts = principalName.contains(":") ? principalName.split(":")
                            : new String[] { principalName };
                    long userId = Long.parseLong(parts[0]);
                    PresenceService presenceService = presenceServiceProvider.getIfAvailable();
                    if (presenceService != null) {
                        log.debug("WebSocket DISCONNECT marking user {} offline", userId);
                        presenceService.setOffline(userId);
                    }
                }
            } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                if (accessor.getUser() != null) {
                    String principalName = accessor.getUser().getName();
                    // Parse userId from principal (format: "userId:orgId")
                    String[] parts = principalName.contains(":") ? principalName.split(":")
                            : new String[] { principalName };
                    long userId = Long.parseLong(parts[0]);
                    PresenceService presenceService = presenceServiceProvider.getIfAvailable();
                    if (presenceService != null) {
                        presenceService.heartbeat(userId);
                    }
                }
            }
        }
        return message;
    }

    // simple principal wrapper with userId and orgId
    public record StompPrincipal(String userId, String orgId) implements Principal {

        @Override
        public String getName() {
            // Return format: "userId:orgId" for use in message handlers
            return userId + ":" + orgId;
        }

    }
}
