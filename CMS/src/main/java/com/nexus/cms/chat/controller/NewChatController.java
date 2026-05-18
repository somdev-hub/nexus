package com.nexus.cms.chat.controller;

import com.nexus.cms.chat.model.ConversationRequestDto;
import com.nexus.cms.chat.service.interfaces.NewChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/cms/chat/v2")
@RequiredArgsConstructor
public class NewChatController {
    private final NewChatService newChatService;

    @PostMapping(value = "/conversation", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createChatConversation(@RequestPart @Valid ConversationRequestDto request, @RequestPart(required = false) MultipartFile chatConversationAvatar){
        return newChatService.createChatConversation(request, chatConversationAvatar);
    }

    @GetMapping("/conversations/{participantId}")
    public ResponseEntity<?> getChatConversation(@PathVariable @NotNull Long participantId){
        return newChatService.getChatConversationMessages(participantId);
    }

    @GetMapping("/conversation/messages")
    public ResponseEntity<?> getChatConversationMessages(@RequestParam Long conversationId, @RequestParam Long participantId, @RequestParam(required = false) Long beforeId, @RequestParam(required = false, defaultValue = "50") Long limit){
        return newChatService.getChatConversationMessages(conversationId, participantId, beforeId, limit);
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<?> getChatConversation(@PathVariable Long conversationId, @RequestParam Long participantId){
        return newChatService.getChatConversation(conversationId, participantId);
    }

    @PutMapping("/conversation")
    public ResponseEntity<?> updateChatConversation(@RequestPart @Valid ConversationRequestDto request, @RequestPart MultipartFile chatConversationAvatar){
        return newChatService.updateChatConversation(request, chatConversationAvatar);
    }
}
