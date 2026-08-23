package com.nexus.nexusbuddy.controller;

import com.nexus.nexusbuddy.annotation.LogActivity;
import com.nexus.nexusbuddy.payload.ChatRequest;
import com.nexus.nexusbuddy.payload.ChatResponse;
import com.nexus.nexusbuddy.service.interfaces.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;

/**
 * REST Controller for Chat operations.
 * Provides chat endpoint for AI-powered conversations.
 */
@RestController
@RequestMapping("/nexusbuddy/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    /**
     * Chat with AI assistant.
     * POST /nexusbuddy/api/chat
     * 
     * @param request Chat request with message and optional context
     * @return 200 OK with AI response
     */
    @PostMapping
    @LogActivity("CHAT")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat request: {}", request.getMessage());
        return chatService.chat(request);
    }

    /**
     * Chat with AI assistant with conversation history.
     * POST /nexusbuddy/api/chat/conversation
     * 
     * @param request Chat request with message and conversation ID
     * @return 200 OK with AI response
     */
    @PostMapping("/conversation")
    @LogActivity("CHAT_CONVERSATION")
    public ResponseEntity<ChatResponse> chatWithConversation(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat conversation request: {}", request.getMessage());
        return chatService.chatWithConversation(request);
    }

    /**
     * Direct chat with AI assistant - simple prompt/response.
     * POST /nexusbuddy/api/chat/direct
     * 
     * @param prompt The prompt to send to AI
     * @return 200 OK with AI response
     */
    @PostMapping("/direct")
    @LogActivity("CHAT_DIRECT")
    public ResponseEntity<ChatResponse> directChat(@RequestBody String prompt) {
        log.info("Received direct chat request: {}", prompt);
        return chatService.directChat(prompt);
    }

    /**
     * Stream chat with AI assistant.
     * POST /nexusbuddy/api/chat/stream
     * 
     * @param request Chat request with message and optional context
     * @return Server-Sent Events stream
     */
    @PostMapping(value = "/stream", produces = "text/event-stream")
    @LogActivity("CHAT_STREAM")
    public Flux<ServerSentEvent<String>> streamChat(@Valid @RequestBody ChatRequest request) {
        log.info("Received streaming chat request: {}", request.getMessage());
        return chatService.streamChat(request);
    }

    /**
     * Get chat health status.
     * GET /nexusbuddy/api/chat/health
     * 
     * @return 200 OK with health status
     */
    @GetMapping("/health")
    @LogActivity("CHAT_HEALTH")
    public ResponseEntity<?> health() {
        log.info("Chat health check requested");
        return ResponseEntity.ok(java.util.Map.of(
                "status", "UP",
                "service", "chat",
                "timestamp", java.time.Instant.now()));
    }

    // ============================================
    // Test/Debug Streaming Endpoint
    // ============================================

    /**
     * Stream dummy logs for testing the chat UI without invoking LLM.
     * POST /nexusbuddy/api/chat/stream/test
     * 
     * @param request Chat request (message is ignored)
     * @return Server-Sent Events stream with dummy log data
     */
    @PostMapping(value = "/stream/test", produces = "text/event-stream")
    @LogActivity("CHAT_STREAM_TEST")
    public Flux<ServerSentEvent<String>> streamTestLogs(@Valid @RequestBody ChatRequest request) {
        log.info("Received test streaming request: {}", request.getMessage());
        return chatService.streamTestLogs(request);
    }

    // ============================================
    // Domain-based Chat APIs
    // ============================================

    /**
     * Stream chat with AI assistant using domain-based tool loading.
     * POST /nexusbuddy/api/chat/stream/by-domain?domain=localhost:3001
     * 
     * @param request Chat request with message and optional context
     * @param domain  Domain to filter by (e.g., "localhost:3001")
     * @return Server-Sent Events stream
     */
    @PostMapping(value = "/stream/by-domain", produces = "text/event-stream")
    @LogActivity("CHAT_STREAM_BY_DOMAIN")
    public Flux<ServerSentEvent<String>> streamChatByDomain(
            @Valid @RequestBody ChatRequest request,
            @RequestParam String domain) {
        log.info("Received streaming chat request for domain: {}", domain);
        return chatService.streamChatByDomain(request, domain);
    }

    /**
     * Chat with AI assistant using domain-based tool loading.
     * POST /nexusbuddy/api/chat/by-domain?domain=localhost:3001
     * 
     * @param request Chat request with message and optional context
     * @param domain  Domain to filter by (e.g., "localhost:3001")
     * @return 200 OK with AI response
     */
    @PostMapping("/by-domain")
    @LogActivity("CHAT_BY_DOMAIN")
    public ResponseEntity<ChatResponse> chatByDomain(
            @Valid @RequestBody ChatRequest request,
            @RequestParam String domain) {
        log.info("Received chat request for domain: {}", domain);
        return chatService.chatByDomain(request, domain);
    }
}