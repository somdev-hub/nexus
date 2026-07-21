package com.nexus.nexusbuddy.service.implementations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.nexusbuddy.ai.DynamicToolProvider;
import com.nexus.nexusbuddy.exception.ConfigNotFoundException;
import com.nexus.nexusbuddy.model.entities.ClientConfig;
import com.nexus.nexusbuddy.model.entities.ToolsConfig;
import com.nexus.nexusbuddy.model.entities.ToolsParamConfig;
import com.nexus.nexusbuddy.payload.ChatMessage;
import com.nexus.nexusbuddy.payload.ChatRequest;
import com.nexus.nexusbuddy.payload.ChatResponse;
import com.nexus.nexusbuddy.repository.ClientConfigRepository;
import com.nexus.nexusbuddy.repository.ToolsConfigRepository;
import com.nexus.nexusbuddy.repository.ToolsParamConfigRepository;
import com.nexus.nexusbuddy.service.interfaces.ChatService;
import com.nexus.nexusbuddy.util.CommonUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for Chat operations using Spring AI with MCP tool calling.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatServiceImpl implements ChatService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ClientConfigRepository clientConfigRepository;
    private final ToolsConfigRepository toolsConfigRepository;
    private final ToolsParamConfigRepository toolsParamConfigRepository;
    private final ChatClient chatClient;
    private final ChatClient.Builder chatClientBuilder;
    private final DynamicToolProvider dynamicToolProvider;

    @Override
    public ResponseEntity<ChatResponse> chat(ChatRequest request) {
        log.info("Processing chat request for client IDs: {}", request.getClientIds());

        CommonUtils.requireNonNull(request, "Chat request");
        CommonUtils.requireNonEmpty(request.getMessage(), "Message");

        List<Long> requestedClientIds = resolveClientIds(request);
        if (requestedClientIds.isEmpty()) {
            throw new IllegalArgumentException("At least one client ID must be provided in clientConfigId or clientIds");
        }

        Set<Long> distinctClientIds = new LinkedHashSet<>(requestedClientIds);
        List<ClientConfig> clientConfigs = clientConfigRepository.findAllById(distinctClientIds);
        if (clientConfigs.size() != distinctClientIds.size()) {
            throw new ConfigNotFoundException("ClientConfig", "clientIds", distinctClientIds);
        }

        List<ToolsConfig> toolsConfigs = toolsConfigRepository.findByIsActiveTrueAndClientConfigClientConfigIdIn(distinctClientIds);

        String systemPrompt = buildSystemPrompt(clientConfigs, toolsConfigs);

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));

        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            for (ChatMessage historyEntry : request.getHistory()) {
                switch (historyEntry.getRole()) {
                    case "user" -> messages.add(new UserMessage(historyEntry.getContent()));
                    case "assistant" -> messages.add(new AssistantMessage(historyEntry.getContent()));
                    case "system" -> messages.add(new SystemMessage(historyEntry.getContent()));
                    default -> log.warn("Unknown history role: {}", historyEntry.getRole());
                }
            }
        }

        messages.add(new UserMessage(request.getMessage()));

        Prompt prompt = new Prompt(messages);
        ToolCallback[] toolCallbacks = dynamicToolProvider.getToolCallbacks(distinctClientIds);
        ChatClient requestChatClient = chatClientBuilder.defaultTools(toolCallbacks).build();
        String aiResponse = requestChatClient.prompt(prompt).call().content();
        String conversationId = UUID.randomUUID().toString();

        ChatResponse response = ChatResponse.builder()
                .message(aiResponse)
                .conversationId(conversationId)
                .timestamp(Instant.now())
                .metadata(request.getMetadata())
                .build();

        log.info("Chat response generated successfully for conversation: {}", conversationId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ChatResponse> chatWithConversation(ChatRequest request) {
        return chat(request);
    }

    @Override
    public ResponseEntity<ChatResponse> directChat(String prompt) {
        log.info("Processing direct chat request: {}", prompt);

        CommonUtils.requireNonEmpty(prompt, "Prompt");

        String systemPrompt = "You are a helpful AI assistant. Provide helpful, accurate, and concise responses.";

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.add(new UserMessage(prompt));

        Prompt aiPrompt = new Prompt(messages);
        String aiResponse = chatClient.prompt(aiPrompt).call().content();
        String conversationId = UUID.randomUUID().toString();

        ChatResponse response = ChatResponse.builder()
                .message(aiResponse)
                .conversationId(conversationId)
                .timestamp(Instant.now())
                .build();

        log.info("Direct chat response generated successfully for conversation: {}", conversationId);
        return ResponseEntity.ok(response);
    }

    private String buildSystemPrompt(List<ClientConfig> clientConfigs, List<ToolsConfig> toolsConfigs) {
        StringBuilder prompt = new StringBuilder();
        if (clientConfigs != null && !clientConfigs.isEmpty()) {
            prompt.append("You are an AI assistant for the following client(s):");
            for (ClientConfig clientConfig : clientConfigs) {
                prompt.append("\n- ").append(clientConfig.getClientName())
                        .append(" (")
                        .append(clientConfig.getClientDescription())
                        .append(")");
            }
            prompt.append("\n\n");
        }

        if (toolsConfigs == null || toolsConfigs.isEmpty()) {
            prompt.append("No external tools are configured for the requested client(s).\n");
        } else {
            prompt.append("You have access to the following tools. Use them only when the user's request requires external data or actions:\n\n");

            for (ToolsConfig tool : toolsConfigs) {
                List<ToolsParamConfig> params = toolsParamConfigRepository.findByToolsConfigToolsConfigId(tool.getToolsConfigId());
                prompt.append("Tool: ").append(tool.getToolName()).append("\n");
                prompt.append("Description: ").append(tool.getToolDescription()).append("\n");
                prompt.append("Endpoint: ").append(tool.getEndpoint()).append("\n");
                prompt.append("HTTP Method: ").append(tool.getHttpMethod()).append("\n");

                if (params != null && !params.isEmpty()) {
                    prompt.append("Parameters:\n");
                    for (ToolsParamConfig param : params) {
                        if (!Boolean.TRUE.equals(param.getIsActive())) {
                            continue;
                        }
                        prompt.append(" - ").append(param.getParamName())
                                .append(" (").append(param.getParamType()).append(", ")
                                .append(param.getDataType()).append(")");
                        if (Boolean.TRUE.equals(param.getIsRequired())) {
                            prompt.append(" [required]");
                        }
                        if (param.getDefaultValue() != null) {
                            prompt.append(" default=").append(param.getDefaultValue());
                        }
                        prompt.append("\n");
                    }
                }
                prompt.append("\n");
            }

            prompt.append("When the user's request requires using one of these tools, call the appropriate tool with the required parameters. ")
                    .append("Do not use tools that are outside the requested client(s).\n");
        }

        prompt.append("\nProvide helpful, accurate, and concise responses.");
        return prompt.toString();
    }

    private List<Long> resolveClientIds(ChatRequest request) {
        List<Long> clientIds = new ArrayList<>();
        if (request.getClientIds() != null) {
            for (Long clientId : request.getClientIds()) {
                if (clientId != null) {
                    clientIds.add(clientId);
                }
            }
        }

        if (clientIds.isEmpty() && request.getClientConfigId() != null) {
            clientIds.add(request.getClientConfigId());
        }

        return clientIds;
    }
}