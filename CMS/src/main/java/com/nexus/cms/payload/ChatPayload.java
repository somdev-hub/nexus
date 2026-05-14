package com.nexus.cms.payload;

import com.nexus.cms.model.entities.Conversation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

/**
 * Chat-related request and response DTOs
 */
public class ChatPayload {

    /**
     * Request to create a new conversation
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateConversationRequest {
        private Conversation.ConversationType type; // DIRECT or GROUP
        private String name; // Optional for DIRECT, required for GROUP
        private List<Long> participantIds; // List of user IDs
        private Long orgId; // Organization ID
    }

    /**
     * Request to send a message
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SendMessageRequest {
        private Long conversationId;
        private String content;
        private Long orgId;
    }

    /**
     * Request to add participant to conversation
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AddParticipantRequest {
        private Long userId;
    }

    /**
     * Typing indicator payload (user is typing or stopped typing)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TypingIndicatorRequest {
        private Long conversationId;
        private Long userId;
    }

    /**
     * Ping/Pong for connection health check
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PingResponse {
        private Long timestamp;
        private String status;
    }

    /**
     * Generic error response
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorResponse {
        private String error;
        private String message;
        private Long timestamp;

        public ErrorResponse(String message) {
            this.message = message;
            this.error = "Error";
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Generic success response
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SuccessResponse {
        private String message;
        private Long timestamp;

        public SuccessResponse(String message) {
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Conversation statistics response
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConversationStats {
        private Long conversationId;
        private Long totalMessages;
        private Long deliveredMessages;
        private Long participantCount;
        private Timestamp createdAt;
    }

    /**
     * User conversation summary (for list view)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConversationSummary {
        private Long id;
        private String name;
        private Conversation.ConversationType type;
        private String lastMessage;
        private Long lastMessageSenderId;
        private Timestamp lastMessageTime;
        private Integer unreadCount;
        private Long participantCount;
    }

    /**
     * Detailed conversation response (for creation and details endpoint)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConversationResponse {
        private Long id;
        private String name;
        private Conversation.ConversationType type;
        private Long createdBy;
        private Long orgId;
        private Boolean isActive;
        private Integer participantCount;
        private Timestamp createdAt;
        private Timestamp updatedAt;
        private Long lastMessageId;
        private Timestamp lastMessageAt;
        private List<ParticipantInfo> participants;
    }

    /**
     * Message with sender info (denormalized for response)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MessageResponse {
        private Long id;
        private Long conversationId;
        private Long senderId;
        private String senderName;
        private String content;
        private Timestamp timestamp;
        private String status;
        private String type;
    }

    /**
     * Conversation with participants list (for detail view)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConversationDetailResponse {
        private Long id;
        private String name;
        private Conversation.ConversationType type;
        private Long createdBy;
        private Timestamp createdAt;
        private List<ParticipantInfo> participants;
        private Integer messageCount;
    }

    /**
     * Participant info (user ID and join time)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ParticipantInfo {
        private Long userId;
        private String userName;
        private Timestamp joinedAt;
        private Boolean isActive;
    }
}
