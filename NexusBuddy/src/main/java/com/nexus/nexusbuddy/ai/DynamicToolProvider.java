package com.nexus.nexusbuddy.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.nexusbuddy.model.entities.ClientConfig;
import com.nexus.nexusbuddy.model.entities.ToolsConfig;
import com.nexus.nexusbuddy.model.entities.ToolsParamConfig;
import com.nexus.nexusbuddy.model.enums.DataType;
import com.nexus.nexusbuddy.model.enums.ParamType;
import com.nexus.nexusbuddy.payload.ToolExecutionResult;
import com.nexus.nexusbuddy.repository.ClientConfigRepository;
import com.nexus.nexusbuddy.repository.ToolsConfigRepository;
import com.nexus.nexusbuddy.repository.ToolsParamConfigRepository;
import com.nexus.nexusbuddy.util.Logger;
import com.nexus.nexusbuddy.util.RestServices;

/**
 * Dynamically builds Spring AI {@link ToolCallback}s from the configured
 * {@link ToolsConfig} and {@link ToolsParamConfig} rows.
 *
 * <p>
 * Tools are loaded on-demand per request based on client IDs, avoiding
 * global tool registration conflicts.
 * </p>
 */
@Component
public class DynamicToolProvider {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DynamicToolProvider.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ToolsConfigRepository toolsConfigRepository;
    private final ToolsParamConfigRepository toolsParamConfigRepository;
    private final ClientConfigRepository clientConfigRepository;
    private final RestServices restServices;
    private final Logger logger;

    public DynamicToolProvider(ToolsConfigRepository toolsConfigRepository,
            ToolsParamConfigRepository toolsParamConfigRepository,
            ClientConfigRepository clientConfigRepository,
            RestServices restServices,
            Logger logger) {
        this.toolsConfigRepository = toolsConfigRepository;
        this.toolsParamConfigRepository = toolsParamConfigRepository;
        this.clientConfigRepository = clientConfigRepository;
        this.restServices = restServices;
        this.logger = logger;
    }

    public ToolCallback[] getToolCallbacks(Collection<Long> clientIds) {
        List<ToolsConfig> activeTools = resolveActiveTools(clientIds);
        if (activeTools.isEmpty()) {
            LOGGER.info("No active tools found in configuration");
            return new ToolCallback[0];
        }

        return activeTools.stream()
                .map(this::buildToolCallback)
                .filter(Objects::nonNull)
                .toArray(ToolCallback[]::new);
    }

    /**
     * Get tool callbacks filtered by domain (e.g., "localhost:3001").
     * This finds client configs that have the domain in their allowedUsersList
     * and returns tools for those client configs.
     * 
     * @param domain The domain to filter by (e.g., "localhost:3001")
     * @return Array of ToolCallbacks for the matching client configs
     */
    public ToolCallback[] getToolCallbacksByDomain(String domain) {
        List<ClientConfig> matchingConfigs = clientConfigRepository.findByAllowedUsersListContaining(domain);
        if (matchingConfigs.isEmpty()) {
            LOGGER.info("No client configs found for domain: {}", domain);
            return new ToolCallback[0];
        }

        List<Long> clientIds = matchingConfigs.stream()
                .map(ClientConfig::getClientConfigId)
                .toList();

        LOGGER.info("Found {} client configs for domain: {}", clientIds.size(), domain);
        return getToolCallbacks(clientIds);
    }

    private List<ToolsConfig> resolveActiveTools(Collection<Long> clientIds) {
        if (clientIds == null || clientIds.isEmpty()) {
            return toolsConfigRepository.findByIsActiveTrue(Pageable.unpaged()).getContent();
        }

        return toolsConfigRepository.findByIsActiveTrueAndClientConfigClientConfigIdIn(clientIds);
    }

    private ToolCallback buildToolCallback(ToolsConfig toolsConfig) {
        try {
            List<ToolsParamConfig> params = toolsParamConfigRepository
                    .findByToolsConfigToolsConfigId(toolsConfig.getToolsConfigId(), Pageable.unpaged())
                    .getContent();

            ToolDefinition definition = ToolDefinition.builder()
                    .name(sanitizeName(toolsConfig.getToolName()))
                    .description(toolsConfig.getToolDescription() != null
                            ? toolsConfig.getToolDescription()
                            : "Tool: " + toolsConfig.getToolName())
                    .inputSchema(buildInputSchemaJson(toolsConfig, params))
                    .build();

            Function<String, String> runner = input -> executeTool(toolsConfig, params, input);

            return new ToolCallback() {
                @Override
                public ToolDefinition getToolDefinition() {
                    return definition;
                }

                @Override
                public String call(String input) {
                    return runner.apply(input);
                }
            };
        } catch (Exception ex) {
            LOGGER.error("Failed to build tool callback for toolName={}, toolsConfigId={}",
                    toolsConfig.getToolName(), toolsConfig.getToolsConfigId(), ex);
            return null;
        }
    }

    private String sanitizeName(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            return "unnamed_tool";
        }
        return toolName.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private String buildInputSchemaJson(ToolsConfig toolsConfig, List<ToolsParamConfig> params) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        for (ToolsParamConfig param : params) {
            if (!Boolean.TRUE.equals(param.getIsActive())) {
                continue;
            }

            Map<String, Object> property = new LinkedHashMap<>();
            property.put("type", mapDataType(param.getDataType()));
            property.put("description",
                    param.getDescription() != null && !param.getDescription().isBlank() ? param.getDescription()
                            : param.getParamName());

            if (param.getDefaultValue() != null) {
                property.put("default", param.getDefaultValue());
            }

            properties.put(param.getParamName(), property);

            if (Boolean.TRUE.equals(param.getIsRequired())) {
                required.add(param.getParamName());
            }
        }

        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(schema);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize tool input schema", ex);
        }
    }

    private String mapDataType(DataType dataType) {
        if (dataType == null) {
            return "string";
        }
        return switch (dataType) {
            case INTEGER -> "integer";
            case STRING -> "string";
            case BOOLEAN -> "boolean";
            case ARRAY -> "array";
            case OBJECT -> "object";
        };
    }

    private String executeTool(ToolsConfig toolsConfig,
            List<ToolsParamConfig> params,
            String inputJson) {
        Map<String, Object> input = parseInput(inputJson);
        validateParameters(params, input);

        Map<String, Object> requestPayload = buildRequestPayload(toolsConfig, params, input);

        String url = resolveUrl(toolsConfig.getEndpoint(), params, input, toolsConfig.getClientConfig());
        HttpMethod method = HttpMethod.valueOf(
                toolsConfig.getHttpMethod() != null
                        ? toolsConfig.getHttpMethod().name()
                        : "GET");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");

        try {
            org.springframework.http.ResponseEntity<?> responseEntity = restServices.nexusBuddyCall(
                    url, requestPayload, headers, method, toolsConfig.getClientConfig(), toolsConfig.getToolName());
            if (responseEntity != null && responseEntity.getBody() != null) {
                return OBJECT_MAPPER.writeValueAsString(responseEntity.getBody());
            }
            return "{\"message\":\"Tool executed successfully with empty response\",\"status\":\"success\"}";
        } catch (Exception ex) {
            LOGGER.error("Tool execution failed for toolName={}", toolsConfig.getToolName(), ex);
            return "{\"message\":\"Tool execution failed: " + ex.getMessage() + "\",\"status\":\"error\"}";
        }
    }

    private Map<String, Object> parseInput(String inputJson) {
        if (inputJson == null || inputJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(inputJson, Map.class);
        } catch (Exception ex) {
            LOGGER.warn("Failed to parse tool input JSON: {}", inputJson, ex);
            return Collections.singletonMap("rawInput", inputJson);
        }
    }

    private void validateParameters(List<ToolsParamConfig> params, Map<String, Object> input) {
        if (params == null || params.isEmpty()) {
            return;
        }

        List<String> missing = new ArrayList<>();
        for (ToolsParamConfig param : params) {
            if (!Boolean.TRUE.equals(param.getIsActive())) {
                continue;
            }
            if (Boolean.TRUE.equals(param.getIsRequired())
                    && (input == null || !input.containsKey(param.getParamName()))) {
                missing.add(param.getParamName());
            }
        }

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    "Missing required parameters for tool: " + String.join(", ", missing));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildRequestPayload(ToolsConfig toolsConfig,
            List<ToolsParamConfig> params,
            Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyMap();
        }

        boolean hasRequestBody = params != null && params.stream()
                .anyMatch(p -> ParamType.REQUEST_BODY.equals(p.getParamType()));

        if (!hasRequestBody) {
            return new LinkedHashMap<>(input);
        }

        for (ToolsParamConfig param : params) {
            if (ParamType.REQUEST_BODY.equals(param.getParamType())
                    && param.getRequestBodyJson() != null) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, Object> template;
                    if (param.getRequestBodyJson() instanceof String) {
                        template = mapper.readValue((String) param.getRequestBodyJson(), Map.class);
                    } else {
                        template = mapper.convertValue(param.getRequestBodyJson(), Map.class);
                    }
                    return mergeTemplateWithInput(template, input);
                } catch (Exception ex) {
                    LOGGER.warn("Failed to parse requestBodyJson for param={}", param.getParamName(), ex);
                }
            }
        }

        return new LinkedHashMap<>(input);
    }

    private Map<String, Object> mergeTemplateWithInput(Map<String, Object> template,
            Map<String, Object> input) {
        Map<String, Object> result = new LinkedHashMap<>(template);
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private String resolveUrl(String endpoint,
            List<ToolsParamConfig> params,
            Map<String, Object> input,
            ClientConfig clientConfig) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("Tool endpoint is not configured");
        }

        String baseUrl = clientConfig != null ? clientConfig.getConnectionUrl() : null;
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "Client connection URL is not configured for clientConfigId="
                            + (clientConfig != null ? clientConfig.getClientConfigId() : "null"));
        }

        String url = baseUrl;
        if (!url.endsWith("/") && !endpoint.startsWith("/")) {
            url = url + "/";
        }
        url = url + endpoint;

        if (params != null && input != null) {
            for (ToolsParamConfig param : params) {
                if (ParamType.PATH_VARIABLE.equals(param.getParamType())
                        && input.containsKey(param.getParamName())) {
                    String placeholder = "{" + param.getParamName() + "}";
                    url = url.replace(placeholder, String.valueOf(input.get(param.getParamName())));
                }
            }
        }
        return url;
    }
}
