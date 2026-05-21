package com.nexus.cms.chat.config;

import com.nexus.cms.payload.TokenPayloadDto;
import com.nexus.cms.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
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
public class ChatWebSocketAuthInceptor implements ChannelInterceptor {
    private final CommonUtils commonUtils;

    @Override
    public @Nullable Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor
                .getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // client sends JWT in the CONNECT frame header
            String token = accessor.getFirstNativeHeader("Authorization");

            if (token != null && token.startsWith("Bearer ")) {
                if (commonUtils.validateToken(token)){
                    TokenPayloadDto tokenPayloadDto = commonUtils.decryptToken(token);
                    accessor.setUser(new StompPrincipal(String.valueOf(tokenPayloadDto.getUserId())));
                }
            } else {
                throw new MessagingException("Missing or invalid Authorization header in WebSocket CONNECT frame");
            }
        }
        return message;
    }

    // simple principal wrapper
    public static class StompPrincipal implements Principal {
        private final String userId;  // userId as string

        public StompPrincipal(String userId) { this.userId = userId; }

        @Override
        public String getName() { return userId; }
    }
}
