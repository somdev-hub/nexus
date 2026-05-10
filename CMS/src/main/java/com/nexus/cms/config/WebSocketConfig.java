package com.nexus.cms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket Configuration for Real-Time Chat
 *
 * Configures STOMP (Simple Text Oriented Messaging Protocol) over WebSocket
 * for bidirectional real-time communication.
 *
 * Key Components:
 * - STOMP Endpoint: /chat/ws - clients connect here
 * - Broker Destinations: /topic/* for broadcasting, /queue/* for unicast
 * - User Destinations: @SendToUser for private messages
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure message broker for STOMP messages
     * Uses in-memory broker for simplicity (can scale to RabbitMQ/ActiveMQ later)
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple in-memory message broker
        // Supports destinations: /topic/*, /queue/*, /user/*
        config.enableSimpleBroker("/topic", "/queue", "/user");

        // Configure: messages with prefix /app will be routed to @MessageMapping
        // Example: client sends to /app/chat/send -> routed to controller
        config.setApplicationDestinationPrefixes("/app");

        // Enable user destination handling
        // Allows @SendToUser to send messages to specific users
        config.setUserDestinationPrefix("/user");
    }

    /**
     * Register STOMP endpoints that clients will use to connect
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint: clients connect via ws://localhost:8086/chat/ws
        registry.addEndpoint("/chat/ws")
                .setAllowedOrigins(
                        "http://localhost:3000"
                )  // Configure CORS as needed
                .withSockJS();           // Fallback to SockJS if WebSocket not supported

        // Additional endpoint without SockJS fallback (pure WebSocket)
        // registry.addEndpoint("/chat/ws-raw")
        //         .setAllowedOrigins("*");
    }
}

