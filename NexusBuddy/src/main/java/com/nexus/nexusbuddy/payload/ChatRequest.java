package com.nexus.nexusbuddy.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request for chat completion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotBlank(message = "Message is required")
    @Size(max = 4000, message = "Message must not exceed 4000 characters")
    private String message;

    private List<ChatMessage> history;

    private String model;

    private Double temperature;

    private Integer maxTokens;

    private Map<String, Object> metadata;
 
    private Long clientConfigId;
 
    private List<Long> clientIds;
 
    private Long toolsConfigId;
}