package com.nexus.cms.chat.controller;

import com.nexus.cms.chat.entities.ChatMessageIndividualStatus;
import com.nexus.cms.chat.model.ChatMessageRequestDto;
import com.nexus.cms.chat.model.StatusUpdateEventDto;
import com.nexus.cms.chat.model.TypingEventDto;
import com.nexus.cms.chat.service.interfaces.MessageService;
import com.nexus.cms.chat.service.interfaces.NewChatService;
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

    @MessageMapping("/cms/chat/ws/message")
    public void sendMessage(
            @DestinationVariable Long conversationId,
            @Payload ChatMessageRequestDto message,
            Principal principal
    ) {
        ResponseEntity<?> responseEntity = messageService.sendMessage(message, null);
        if (responseEntity.getStatusCode().is2xxSuccessful() && !ObjectUtils.isEmpty(responseEntity.getBody())) {
            // broadcast the new message to all subscribers of the conversation topic
            messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, responseEntity.getBody());
        } else {
            // handle error case, e.g. send an error message back to the sender
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", "Failed to send message: " + responseEntity.getStatusCode());
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
    public void acknowledge(@DestinationVariable Long messageId, Principal principal){
        long userId = Long.parseLong(principal.getName());
        ResponseEntity<ChatMessageIndividualStatus> response = newChatService.markReceived(messageId, userId);
        if (!response.getStatusCode().is2xxSuccessful() || ObjectUtils.isEmpty(response.getBody())){
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", "Failed to acknowledge message: " + response.getStatusCode());
            return;
        }
        messagingTemplate.convertAndSend("/topic/conversation/"+response.getBody().getParticipant().getChatConversation().getChatConversationId(), new StatusUpdateEventDto(messageId, response.getBody().getChatMessage().getChatMessageStatus()));
    }


}
