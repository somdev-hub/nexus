package com.nexus.cms.chat.controller;

import com.nexus.cms.chat.model.ChatMessageRequestDto;
import com.nexus.cms.chat.model.ConversationMessagesDto;
import com.nexus.cms.chat.model.MessageSentEventDto;
import com.nexus.cms.chat.service.interfaces.ChatMessageProducerService;
import com.nexus.cms.chat.service.interfaces.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.security.Principal;

@RestController
@RequestMapping("/cms/chat/v2/message")
@RequiredArgsConstructor
@Slf4j
public class MessageController {
    private final MessageService messageService;
    private final ChatMessageProducerService chatMessageProducerService;

    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> sendMessage(@RequestPart @Valid ChatMessageRequestDto message,
            @RequestPart(required = false) List<MultipartFile> files,
            Principal principal) {
        Long participantId;
        if (principal != null && principal.getName() != null) {
            // Called from frontend with JWT principal
            participantId = Long.parseLong(principal.getName().split(":")[0]);
        } else if (message.getParticipantId() != null) {
            // Called from IAM gateway with participantId in DTO
            participantId = message.getParticipantId();
        } else {
            return ResponseEntity.status(401).build();
        }
        message.setParticipantId(participantId);
        ResponseEntity<?> response = messageService.sendMessage(message, files, participantId);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() instanceof ConversationMessagesDto body) {
            try {
                chatMessageProducerService.publishMessageSent(MessageSentEventDto.builder()
                        .messageId(body.getChatMessageId())
                        .conversationId(body.getChatConversationId())
                        .participantId(body.getChatConversationParticipant() != null ? body.getChatConversationParticipant().getParticipantId() : participantId)
                        .message(body)
                        .build());
            } catch (Exception e) {
                log.warn("Failed to publish message sent event", e);
            }
        }
        return response;
    }

    @PostMapping(value = "/multimedia", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> sendMultimedia(
            @RequestPart List<MultipartFile> files,
            @RequestParam Long participantId) {
        return messageService.sendMultimediaMessage(files, participantId);
    }

    @PutMapping("/")
    public ResponseEntity<?> editMessage(@RequestBody @Valid ChatMessageRequestDto message) {
        return messageService.editMessage(message);
    }
}
