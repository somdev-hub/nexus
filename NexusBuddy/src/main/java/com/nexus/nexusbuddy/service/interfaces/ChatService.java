package com.nexus.nexusbuddy.service.interfaces;

import com.nexus.nexusbuddy.payload.ChatRequest;
import com.nexus.nexusbuddy.payload.ChatResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * Service interface for Chat operations.
 */
public interface ChatService {

    /**
     * Simple chat without conversation history.
     * 
     * @param request Chat request
     * @return Chat response
     */
    ResponseEntity<ChatResponse> chat(ChatRequest request);

    /**
     * Chat with conversation history.
     * 
     * @param request Chat request with conversation ID
     * @return Chat response
     */
    ResponseEntity<ChatResponse> chatWithConversation(ChatRequest request);

    /**
     * Direct chat - takes a simple prompt string and returns AI response.
     * No client config, tools, or conversation history needed.
     * 
     * @param prompt The user prompt
     * @return Chat response with AI-generated content
     */
    ResponseEntity<ChatResponse> directChat(String prompt);

    /**
     * Stream chat with AI assistant.
     * 
     * @param request Chat request with message and optional context
     * @return Flux of Server-Sent Events
     */
    Flux<ServerSentEvent<String>> streamChat(ChatRequest request);

    /**
     * Stream chat with AI assistant using domain-based tool loading.
     * Finds client configs that have the domain in their allowedUsersList
     * and loads tools for those client configs.
     * 
     * @param request Chat request with message and optional context
     * @param domain  Domain to filter by (e.g., "localhost:3001")
     * @return Flux of Server-Sent Events
     */
    Flux<ServerSentEvent<String>> streamChatByDomain(ChatRequest request, String domain);

    /**
     * Non-streaming chat with AI assistant using domain-based tool loading.
     * Finds client configs that have the domain in their allowedUsersList
     * and loads tools for those client configs.
     * 
     * @param request Chat request with message and optional context
     * @param domain  Domain to filter by (e.g., "localhost:3001")
     * @return Chat response
     */
    ResponseEntity<ChatResponse> chatByDomain(ChatRequest request, String domain);

    /**
     * Stream dummy logs for testing the chat UI without invoking LLM.
     * 
     * @param request Chat request (message is ignored)
     * @return Flux of Server-Sent Events with dummy log data
     */
    Flux<ServerSentEvent<String>> streamTestLogs(ChatRequest request);
}