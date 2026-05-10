package com.nexus.iam.service;

import org.springframework.http.ResponseEntity;

public interface ChatService {
    ResponseEntity<?> createConversation(String request, Long userId);

    ResponseEntity<?> getUserConversations(int page, int size, Long orgId, Long userId);

    ResponseEntity<?> getConversation(String conversationId, Long orgId, Long userId);

    ResponseEntity<?> getConversationMessages(String conversationId, Long orgId, int page, int size, Long userId);

    ResponseEntity<?> addParticipant(String conversationId, String request, Long orgId);

    ResponseEntity<?> removeParticipant(String conversationId, Long userId, Long orgId);

    ResponseEntity<?> sendMessage(String request, Long orgId, Long userId);

    ResponseEntity<?> getMessage(String messageId, Long orgId);

    ResponseEntity<?> getConversationStats(String conversationId, Long orgId);
}
