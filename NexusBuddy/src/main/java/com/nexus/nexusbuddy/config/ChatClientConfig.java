package com.nexus.nexusbuddy.config;

import com.nexus.nexusbuddy.ai.DynamicToolProvider;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Spring AI Chat Client and MCP tool integration.
 */
@Configuration
public class ChatClientConfig {

    @Bean
    ChatClient chatClient(org.springframework.ai.chat.client.ChatClient.Builder builder,
        	DynamicToolProvider dynamicToolProvider) {
        ToolCallback[] toolCallbacks = dynamicToolProvider.getToolCallbacks();
        return builder.defaultTools(toolCallbacks).build();
    }
}
