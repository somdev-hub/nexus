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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class MessageWebSocketController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NewChatService newChatService;
    private final ChatMessageProducerService chatMessageProducerService;
    private final PresenceService presenceService;

    @MessageMapping("/cms/chat/ws/message/{conversationId}")
    public void sendMessage(
            @DestinationVariable Long conversationId,
            @Payload ChatMessageRequestDto message,
            Principal principal) {
        try {
            // Extract userId from principal name (format: "userId:orgId")
            String principalName = principal.getName();
            Long senderUserId = Long.parseLong(principalName.split(":")[0]);

            // Never trust client-sent sender/conversation values for WebSocket SEND.
            if (!ObjectUtils.isEmpty(message)) {
                message.setChatConversationId(conversationId);
                message.setParticipantId(senderUserId);
            }

            log.info("[CHAT WS] Incoming message SEND - conversationId={}, senderUserId={}",
                    conversationId, senderUserId);

            ResponseEntity<ChatMessage> responseEntity = messageService.sendMessage(message, null, senderUserId);

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                ChatMessage body = responseEntity.getBody();
                if (!ObjectUtils.isEmpty(body) && !ObjectUtils.isEmpty(body.getChatMessageId())) {
                    chatMessageProducerService.publishMessageSent(MessageSentEventDto.builder()
                            .messageId(body.getChatMessageId())
                            .conversationId(body.getChatConversation().getChatConversationId())
                            .participantId(body.getChatConversationParticipant().getParticipantId()).message(body)
                            .build());
                    log.info("[CHAT WS] Message persisted and published - conversationId={}, messageId={}",
                            body.getChatConversation().getChatConversationId(), body.getChatMessageId());
                } else {
                    log.warn("[CHAT WS] Message persisted but response body/messageId is empty - conversationId={}",
                            conversationId);
                }
            } else {
                log.warn("[CHAT WS] Message rejected by service - conversationId={}, senderUserId={}, status={}",
                        conversationId, senderUserId, responseEntity.getStatusCode());
            }
        } catch (Exception ex) {
            log.error("[CHAT WS] Failed processing SEND frame - conversationId={}, error={}",
                    conversationId, ex.getMessage(), ex);
            throw ex;
        }
    }

    // typing indicator
    @MessageMapping("/cms/chat/ws/typing/{conversationId}")
    public void sendTypingIndicator(
            @DestinationVariable Long conversationId,
            Principal principal) {
        // Extract userId from principal name (format: "userId:orgId")
        String principalName = principal.getName();
        long userId = Long.parseLong(principalName.contains(":") ? principalName.split(":")[0] : principalName);
        // broadcast typing indicator to all subscribers of the conversation topic
        // except the sender
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId + "/typing",
                new TypingEventDto(userId, true));
    }

    // acknowledgement
    @MessageMapping("/cms/chat/ws/ack/{messageId}")
    public void acknowledge(@DestinationVariable Long messageId, Principal principal) {
        // Extract userId from principal name (format: "userId:orgId")
        String principalName = principal.getName();
        long userId = Long.parseLong(principalName.contains(":") ? principalName.split(":")[0] : principalName);
        ResponseEntity<ChatMessageIndividualStatus> response = newChatService.markReceived(messageId, userId);
        if (!response.getStatusCode().is2xxSuccessful() || ObjectUtils.isEmpty(response.getBody())) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors",
                    "Failed to acknowledge message: " + response.getStatusCode());
            return;
        }
        messagingTemplate.convertAndSend(
                "/topic/conversations/"
                        + response.getBody().getParticipant().getChatConversation().getChatConversationId(),
                new StatusUpdateEventDto(messageId, response.getBody().getChatMessage().getChatMessageStatus()));
    }

    @MessageMapping("/heartbeat")
    public void heartbeat(Principal principal) {
        // Extract userId from principal name (format: "userId:orgId")
        String principalName = principal.getName();
        Long userId = Long.parseLong(principalName.contains(":") ? principalName.split(":")[0] : principalName);
        presenceService.heartbeat(userId); // ← refresh TTL
    }

}
