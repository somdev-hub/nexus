package com.nexus.cms.chat.controller;

import com.nexus.cms.chat.entities.ChatMessage;
import com.nexus.cms.chat.entities.ChatMessageIndividualStatus;
import com.nexus.cms.chat.model.ChatMessageRequestDto;
import com.nexus.cms.chat.model.MessageSentEventDto;
import com.nexus.cms.chat.model.StatusUpdateEventDto;
import com.nexus.cms.chat.model.TypingEventDto;
import com.nexus.cms.chat.service.interfaces.ChatMessageProducerService;
import com.nexus.cms.chat.service.interfaces.MessageService;
import com.nexus.cms.chat.service.interfaces.NewChatService;
import com.nexus.cms.chat.service.interfaces.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.ObjectUtils;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class MessageWebSocketController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NewChatService newChatService;
    private final ChatMessageProducerService chatMessageProducerService;
    private final PresenceService presenceService;

    @MessageMapping("/cms/chat/ws/message")
    public void sendMessage(
            @DestinationVariable Long conversationId,
            @Payload ChatMessageRequestDto message,
            Principal principal
    ) {
        ResponseEntity<ChatMessage> responseEntity = messageService.sendMessage(message, null);

        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            ChatMessage body = responseEntity.getBody();
            if (!ObjectUtils.isEmpty(body) && !ObjectUtils.isEmpty(body.getChatMessageId())) {
                chatMessageProducerService.publishMessageSent(MessageSentEventDto.builder().messageId(body.getChatMessageId()).conversationId(body.getChatConversation().getChatConversationId()).participantId(body.getChatConversationParticipant().getParticipantId()).message(body).build());
            }
        }
    }

    // typing indicator
    @MessageMapping("/cms/chat/ws/typing")
    public void sendTypingIndicator(
            @DestinationVariable Long conversationId,
            Principal principal
    ) {
        // broadcast typing indicator to all subscribers of the conversation topic except the sender
        messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, new TypingEventDto(Long.parseLong(principal.getName()), true));
    }

    //acknowledgement
    @MessageMapping("/cms/chat/ws/ack")
    public void acknowledge(@DestinationVariable Long messageId, Principal principal) {
        long userId = Long.parseLong(principal.getName());
        ResponseEntity<ChatMessageIndividualStatus> response = newChatService.markReceived(messageId, userId);
        if (!response.getStatusCode().is2xxSuccessful() || ObjectUtils.isEmpty(response.getBody())) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", "Failed to acknowledge message: " + response.getStatusCode());
            return;
        }
        messagingTemplate.convertAndSend("/topic/conversation/" + response.getBody().getParticipant().getChatConversation().getChatConversationId(), new StatusUpdateEventDto(messageId, response.getBody().getChatMessage().getChatMessageStatus()));
    }

    @MessageMapping("/heartbeat")
    public void heartbeat(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        presenceService.heartbeat(userId);  // ← refresh TTL
    }


}
