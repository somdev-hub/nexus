package com.nexus.cms.chat.controller;

import com.nexus.cms.chat.model.ConversationRequestDto;
import com.nexus.cms.chat.service.interfaces.NewChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/cms/chat/v2")
@RequiredArgsConstructor
public class NewChatController {
    private final NewChatService newChatService;

    @PostMapping(value = "/conversation")
    public ResponseEntity<?> createChatConversation(@RequestBody @Valid ConversationRequestDto request){
        return newChatService.createChatConversation(request, null);
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
    public ResponseEntity<?> updateChatConversation(@RequestPart ConversationRequestDto request, @RequestPart(required = false) MultipartFile chatConversationAvatar){
        return newChatService.updateChatConversation(request, chatConversationAvatar);
    }


    @PostMapping("/view")
    public ResponseEntity<?> viewConversation(@RequestParam Long participantId, @RequestParam Long conversationId){
        return newChatService.viewConversation(participantId, conversationId);
    }

    @PostMapping("/conversation/{conversationId}/participants")
    public ResponseEntity<?> addParticipants(@PathVariable Long conversationId, @RequestBody java.util.List<com.nexus.cms.chat.model.ChatConversationParticipantDto> participants) {
        return newChatService.addParticipants(conversationId, participants);
    }

    @DeleteMapping("/conversation/{conversationId}/participants/{participantId}")
    public ResponseEntity<?> removeParticipant(@PathVariable Long conversationId, @PathVariable Long participantId) {
        return newChatService.removeParticipant(conversationId, participantId);
    }

    @PutMapping("/conversation/{conversationId}/participants/{participantId}/admin")
    public ResponseEntity<?> setAdmin(@PathVariable Long conversationId, @PathVariable Long participantId, @RequestParam Long adminId, @RequestParam boolean isAdmin) {
        return newChatService.setAdmin(conversationId, participantId, adminId, isAdmin);
    }

    @DeleteMapping("/conversation/{conversationId}")
    public ResponseEntity<?> deleteConversation(@PathVariable Long conversationId, @RequestParam Long participantId) {
        return newChatService.deleteConversation(conversationId, participantId);
    }

    @PostMapping("/conversation/{conversationId}/clear")
    public ResponseEntity<?> clearChatForSelf(@PathVariable Long conversationId, @RequestParam Long participantId) {
        return newChatService.clearChatForSelf(conversationId, participantId);
    }

    @GetMapping("/conversation/{conversationId}/download")
    public ResponseEntity<?> downloadChat(@PathVariable Long conversationId, @RequestParam Long participantId) {
        return newChatService.downloadChat(conversationId, participantId);
    }

    @GetMapping("/conversation/{conversationId}/search")
    public ResponseEntity<?> searchMessages(@PathVariable Long conversationId, @RequestParam Long participantId, @RequestParam String query) {
        return newChatService.searchMessages(conversationId, participantId, query);
    }

    @DeleteMapping("/message/{messageId}")
    public ResponseEntity<?> deleteMessage(
            @PathVariable Long messageId,
            @RequestParam Long participantId,
            @RequestParam(required = false, defaultValue = "true") boolean deleteForEveryone) {
        return newChatService.deleteMessage(messageId, participantId, deleteForEveryone);
    }

    @PostMapping("/message/{messageId}/pin")
    public ResponseEntity<?> pinMessage(
            @PathVariable Long messageId,
            @RequestParam Long participantId) {
        return newChatService.pinMessage(messageId, participantId);
    }

    @PostMapping("/message/{messageId}/unpin")
    public ResponseEntity<?> unpinMessage(
            @PathVariable Long messageId,
            @RequestParam Long participantId) {
        return newChatService.unpinMessage(messageId, participantId);
    }

    @GetMapping("/conversation/{conversationId}/pinned")
    public ResponseEntity<?> getPinnedMessage(
            @PathVariable Long conversationId,
            @RequestParam Long participantId) {
        return newChatService.getPinnedMessage(conversationId, participantId);
    }

    @PostMapping("/message/reply")
    public ResponseEntity<?> sendMessageWithReply(
            @RequestParam Long conversationId,
            @RequestParam Long participantId,
            @RequestParam String content,
            @RequestParam(required = false) Long repliedToMessageId) {
        return newChatService.sendMessageWithReply(conversationId, participantId, content, repliedToMessageId);
    }
}
