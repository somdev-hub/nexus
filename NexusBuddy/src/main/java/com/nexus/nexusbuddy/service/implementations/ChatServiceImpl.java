package com.nexus.nexusbuddy.service.implementations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

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

    @Override
    public ResponseEntity<ChatResponse> chat(ChatRequest request) {
        log.info("Processing chat request for client config ID: {}", request.getClientConfigId());

        CommonUtils.requireNonNull(request, "Chat request");
        CommonUtils.requireNonEmpty(request.getMessage(), "Message");
        CommonUtils.requireNonNull(request.getClientConfigId(), "Client config ID");

        ClientConfig clientConfig = clientConfigRepository.findById(request.getClientConfigId())
                .orElseThrow(() -> new ConfigNotFoundException("ClientConfig", "clientConfigId", request.getClientConfigId()));

        List<ToolsConfig> toolsConfigs = toolsConfigRepository.findByClientConfigClientConfigId(request.getClientConfigId());

        String systemPrompt = buildSystemPrompt(clientConfig, toolsConfigs);

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
        String aiResponse = chatClient.prompt(prompt).call().content();
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

    private String buildSystemPrompt(ClientConfig clientConfig, List<ToolsConfig> toolsConfigs) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI assistant for ").append(clientConfig.getClientName()).append(".\n");
        prompt.append("Client Description: ").append(clientConfig.getClientDescription()).append("\n\n");

        if (toolsConfigs == null || toolsConfigs.isEmpty()) {
            prompt.append("No external tools are configured for this client.\n");
        } else {
            prompt.append("You have access to the following tools. Use them when the user's request requires external data or actions:\n\n");

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
                    .append("If required parameters are missing, ask the user to provide them before calling the tool.\n");
        }

        prompt.append("\nProvide helpful, accurate, and concise responses.");
        return prompt.toString();
    }
}