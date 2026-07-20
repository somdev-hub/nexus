package com.nexus.nexusbuddy.service.interfaces;

import com.nexus.nexusbuddy.payload.ChatRequest;
import com.nexus.nexusbuddy.payload.ChatResponse;
import org.springframework.http.ResponseEntity;

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
}