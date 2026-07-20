package com.nexus.nexusbuddy.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chat message for conversation history.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @NotNull(message = "Role is required")
    private String role; // user, assistant, system

    @NotBlank(message = "Content is required")
    private String content;

    private String name; // optional name for the message sender
}