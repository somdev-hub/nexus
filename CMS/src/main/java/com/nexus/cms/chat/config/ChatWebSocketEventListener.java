package com.nexus.cms.chat.config;

import com.nexus.cms.chat.entities.ChatParticipantsPresence;
import com.nexus.cms.chat.model.PresenceEventDto;
import com.nexus.cms.chat.repositories.ChatParticipantsPresenceRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.sql.Timestamp;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ChatWebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatParticipantsPresenceRepo chatParticipantsPresenceRepo;

    @EventListener
    public void handleConnect(SessionConnectedEvent event){
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = extractUserId(accessor);
        if (chatParticipantsPresenceRepo.existsByUserId(userId)){
            chatParticipantsPresenceRepo.updatePresenceStatus(userId, true);
        } else {
            ChatParticipantsPresence chatParticipantsPresence=new ChatParticipantsPresence();
            chatParticipantsPresence.setUserId(userId);
            chatParticipantsPresence.setIsOnline(true);
            chatParticipantsPresence.setLastActiveAt(new Timestamp(System.currentTimeMillis()));
            chatParticipantsPresenceRepo.save(chatParticipantsPresence);
        }
        messagingTemplate.convertAndSend("/topic/presence", new PresenceEventDto(userId, PresenceEventDto.PresenceEventStatus.ONLINE, new Timestamp(System.currentTimeMillis())));
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event){
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = extractUserId(accessor);
        if (chatParticipantsPresenceRepo.existsByUserId(userId)){
            chatParticipantsPresenceRepo.updatePresenceStatus(userId, false);
        }
        messagingTemplate.convertAndSend("/topic/presence", new PresenceEventDto(userId, PresenceEventDto.PresenceEventStatus.OFFLINE, new Timestamp(System.currentTimeMillis())));
    }

    private Long extractUserId(StompHeaderAccessor accessor) {
        return Long.parseLong(Objects.requireNonNull(accessor.getUser()).getName());
    }
}
