package com.nexus.nexusbuddy.service.implementations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.nexusbuddy.model.entities.ClientConfig;
import com.nexus.nexusbuddy.model.entities.ToolsConfig;
import com.nexus.nexusbuddy.model.entities.ToolsParamConfig;
import com.nexus.nexusbuddy.model.enums.DataType;
import com.nexus.nexusbuddy.model.enums.ParamType;
import com.nexus.nexusbuddy.model.enums.ToolsHttpMethod;
import com.nexus.nexusbuddy.repository.ClientConfigRepository;
import com.nexus.nexusbuddy.repository.ToolsConfigRepository;
import com.nexus.nexusbuddy.repository.ToolsParamConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service to seed RecruitmentController GET APIs into NexusBuddy tools and
 * params tables.
 * Reads API definitions from JSON configuration file and persists them to the
 * database.
 * Uses existing HR Service client (clientConfigId: 1).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecruitmentApiSeederService {

    private final ClientConfigRepository clientConfigRepository;
    private final ToolsConfigRepository toolsConfigRepository;
    private final ToolsParamConfigRepository toolsParamConfigRepository;
    private final ObjectMapper objectMapper;

    /**
     * Seeds all GET APIs from RecruitmentController into the database using JSON
     * configuration.
     * Creates tools config and param configs for each endpoint under the existing
     * HR Service client.
     * 
     * @return Summary of seeded data
     */
    @Transactional
    public Map<String, Object> seedRecruitmentApis() {
        return seedRecruitmentApis(null);
    }

    /**
     * Seeds recruitment APIs from JSON configuration. Can accept custom JSON input
     * or use the default classpath resource.
     * 
     * @param customJsonNode Optional custom JSON configuration. If null, uses
     *                       default resource.
     * @return Summary of seeded data
     */
    @Transactional
    public Map<String, Object> seedRecruitmentApis(JsonNode customJsonNode) {
        log.info("Starting RecruitmentController GET API seeding...");

        // 1. Get existing HR Client Config (clientConfigId: 1)
        ClientConfig hrClient = clientConfigRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException(
                        "HR Service client config with ID 1 not found. Please ensure the client exists."));
        log.info("Using existing HR Client Config ID: {}", hrClient.getClientConfigId());

        // 2. Load endpoint definitions from JSON
        JsonNode toolsNode;
        if (customJsonNode != null) {
            toolsNode = customJsonNode.get("tools");
            if (toolsNode == null || !toolsNode.isArray()) {
                throw new IllegalArgumentException("Invalid JSON: 'tools' array not found");
            }
        } else {
            toolsNode = loadDefaultConfiguration();
        }

        int toolsCreated = 0;
        int paramsCreated = 0;
        int toolsSkipped = 0;
        List<String> createdTools = new ArrayList<>();
        List<String> skippedTools = new ArrayList<>();

        // 3. Process each tool definition
        for (JsonNode toolNode : toolsNode) {
            String toolName = toolNode.get("toolName").asText();

            // Check if tool already exists
            if (toolsConfigRepository.existsByToolName(toolName)) {
                log.info("Tool '{}' already exists, skipping...", toolName);
                toolsSkipped++;
                skippedTools.add(toolName);
                continue;
            }

            // Create ToolsConfig
            ToolsConfig toolConfig = ToolsConfig.builder()
                    .toolName(toolName)
                    .toolDescription(toolNode.get("toolDescription").asText())
                    .endpoint(toolNode.get("endpoint").asText())
                    .httpMethod(ToolsHttpMethod.valueOf(toolNode.get("httpMethod").asText()))
                    .isActive(true)
                    .clientConfig(hrClient)
                    .build();

            ToolsConfig savedTool = toolsConfigRepository.save(toolConfig);
            toolsCreated++;
            createdTools.add(toolName);
            log.info("Created tool: {} (ID: {})", savedTool.getToolName(), savedTool.getToolsConfigId());

            // Create ParamConfigs for this tool
            JsonNode paramsNode = toolNode.get("params");
            if (paramsNode != null && paramsNode.isArray()) {
                for (JsonNode paramNode : paramsNode) {
                    ToolsParamConfig paramConfig = ToolsParamConfig.builder()
                            .paramName(paramNode.get("paramName").asText())
                            .paramType(ParamType.valueOf(paramNode.get("paramType").asText()))
                            .dataType(DataType.valueOf(paramNode.get("dataType").asText()))
                            .isRequired(paramNode.get("isRequired").asBoolean())
                            .defaultValue(parseDefaultValue(paramNode.get("defaultValue"),
                                    paramNode.get("dataType").asText()))
                            .description(paramNode.get("description").asText())
                            .isActive(true)
                            .toolsConfig(savedTool)
                            .build();

                    toolsParamConfigRepository.save(paramConfig);
                    paramsCreated++;
                    log.info("  Created param: {} ({})", paramConfig.getParamName(), paramConfig.getParamType());
                }
            }
        }

        log.info("Seeding completed. Tools created: {}, Params created: {}, Tools skipped: {}",
                toolsCreated, paramsCreated, toolsSkipped);

        return Map.of(
                "clientConfigId", hrClient.getClientConfigId(),
                "clientName", hrClient.getClientName(),
                "toolsCreated", toolsCreated,
                "paramsCreated", paramsCreated,
                "toolsSkipped", toolsSkipped,
                "createdTools", createdTools,
                "skippedTools", skippedTools);
    }

    /**
     * Loads the default JSON configuration from classpath.
     */
    private JsonNode loadDefaultConfiguration() {
        try {
            ClassPathResource resource = new ClassPathResource("seeder/recruitment-apis.json");
            try (InputStream inputStream = resource.getInputStream()) {
                JsonNode rootNode = objectMapper.readTree(inputStream);
                JsonNode toolsNode = rootNode.get("tools");
                if (toolsNode == null || !toolsNode.isArray()) {
                    throw new IllegalStateException("Invalid default configuration: 'tools' array not found");
                }
                return toolsNode;
            }
        } catch (Exception e) {
            log.error("Failed to load default recruitment APIs configuration", e);
            throw new IllegalStateException("Failed to load default configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Parses default value from JSON node based on data type.
     */
    private Object parseDefaultValue(JsonNode defaultValueNode, String dataType) {
        if (defaultValueNode == null || defaultValueNode.isNull()) {
            return null;
        }

        try {
            switch (dataType) {
                case "INTEGER":
                    return defaultValueNode.asInt();
                case "BOOLEAN":
                    return defaultValueNode.asBoolean();
                case "STRING":
                    return defaultValueNode.asText();
                default:
                    // For ARRAY, OBJECT or unknown types, return as JSON string
                    return defaultValueNode.toString();
            }
        } catch (Exception e) {
            log.warn("Failed to parse default value for type {}: {}", dataType, e.getMessage());
            return defaultValueNode.toString();
        }
    }
}