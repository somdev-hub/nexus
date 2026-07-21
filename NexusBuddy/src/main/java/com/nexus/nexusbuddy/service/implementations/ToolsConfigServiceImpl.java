package com.nexus.nexusbuddy.service.implementations;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.nexusbuddy.exception.ConfigNotFoundException;
import com.nexus.nexusbuddy.exception.ConfigValidationException;
import com.nexus.nexusbuddy.model.entities.ClientConfig;
import com.nexus.nexusbuddy.model.entities.ToolsConfig;
import com.nexus.nexusbuddy.model.entities.ToolsParamConfig;
import com.nexus.nexusbuddy.payload.ToolsConfigRequest;
import com.nexus.nexusbuddy.payload.ToolsConfigResponse;
import com.nexus.nexusbuddy.repository.ClientConfigRepository;
import com.nexus.nexusbuddy.repository.ToolsConfigRepository;
import com.nexus.nexusbuddy.repository.ToolsParamConfigRepository;
import com.nexus.nexusbuddy.service.interfaces.ToolsConfigService;
import com.nexus.nexusbuddy.util.CommonUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for Tools Config management.
 * Provides CRUD operations for tool configurations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ToolsConfigServiceImpl implements ToolsConfigService {

    private final ToolsConfigRepository toolsConfigRepository;
    private final ClientConfigRepository clientConfigRepository;
    private final ToolsParamConfigRepository toolsParamConfigRepository;
    private final ModelMapper modelMapper;

    @Override
    public ResponseEntity<?> createToolsConfig(ToolsConfigRequest request) {
        log.info("Creating new tools config: {}", request.getToolName());
        
        CommonUtils.requireNonNull(request, "Tools config request");
        CommonUtils.requireNonEmpty(request.getToolName(), "Tool name");
        CommonUtils.requireNonEmpty(request.getEndpoint(), "Endpoint");
        CommonUtils.requireNonNull(request.getHttpMethod(), "HTTP method");
        CommonUtils.requireNonNull(request.getIsActive(), "Active status");
        CommonUtils.requireNonNull(request.getClientConfigId(), "Client config ID");

        ClientConfig clientConfig = clientConfigRepository.findById(request.getClientConfigId())
                .orElseThrow(() -> new ConfigNotFoundException("ClientConfig", "clientConfigId", request.getClientConfigId()));

        if (toolsConfigRepository.existsByToolName(request.getToolName())) {
            throw new ConfigValidationException("Tools config with name '" + request.getToolName() + "' already exists");
        }

        ToolsConfig toolsConfig = modelMapper.map(request, ToolsConfig.class);
        toolsConfig.setClientConfig(clientConfig);

        ToolsConfig savedConfig = toolsConfigRepository.save(toolsConfig);

        // Save param configs if provided
        if (request.getParamConfigs() != null && !request.getParamConfigs().isEmpty()) {
            List<ToolsParamConfig> paramConfigs = request.getParamConfigs().stream()
                    .map(paramRequest -> {
                        ToolsParamConfig paramConfig = modelMapper.map(paramRequest, ToolsParamConfig.class);
                        paramConfig.setToolsConfig(savedConfig);
                        return paramConfig;
                    })
                    .toList();
            toolsParamConfigRepository.saveAll(paramConfigs);
            savedConfig.setToolsParamConfigList(paramConfigs);
        }

        log.info("Tools config created successfully with ID: {}", savedConfig.getToolsConfigId());

        return ResponseEntity.status(201).body(modelMapper.map(savedConfig, ToolsConfigResponse.class));
    }

    @Override
    public ResponseEntity<?> getToolsConfigById(Long toolsConfigId) {
        log.info("Fetching tools config with ID: {}", toolsConfigId);
        
        CommonUtils.requireNonNull(toolsConfigId, "Tools config ID");

        ToolsConfig toolsConfig = toolsConfigRepository.findById(toolsConfigId)
                .orElseThrow(() -> new ConfigNotFoundException("ToolsConfig", "toolsConfigId", toolsConfigId));

        return ResponseEntity.ok(modelMapper.map(toolsConfig, ToolsConfigResponse.class));
    }

    @Override
    public ResponseEntity<?> getAllToolsConfigs() {
        log.info("Fetching all tools configs");
        
        List<ToolsConfig> configs = toolsConfigRepository.findAll();
        List<ToolsConfigResponse> responses = configs.stream()
                .map(config -> modelMapper.map(config, ToolsConfigResponse.class))
                .toList();

        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<?> getActiveToolsConfigs() {
        log.info("Fetching active tools configs");
        
        List<ToolsConfig> configs = toolsConfigRepository.findByIsActiveTrue();
        List<ToolsConfigResponse> responses = configs.stream()
                .map(config -> modelMapper.map(config, ToolsConfigResponse.class))
                .toList();

        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<?> getToolsConfigsByClientConfigId(Long clientConfigId) {
        log.info("Fetching tools configs for client config ID: {}", clientConfigId);
        
        CommonUtils.requireNonNull(clientConfigId, "Client config ID");

        if (!clientConfigRepository.existsById(clientConfigId)) {
            throw new ConfigNotFoundException("ClientConfig", "clientConfigId", clientConfigId);
        }

        List<ToolsConfig> configs = toolsConfigRepository.findByClientConfigClientConfigId(clientConfigId);
        List<ToolsConfigResponse> responses = configs.stream()
                .map(config -> modelMapper.map(config, ToolsConfigResponse.class))
                .toList();

        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<?> updateToolsConfig(Long toolsConfigId, ToolsConfigRequest request) {
        log.info("Updating tools config with ID: {}", toolsConfigId);
        
        CommonUtils.requireNonNull(toolsConfigId, "Tools config ID");
        CommonUtils.requireNonNull(request, "Tools config request");

        ToolsConfig existingConfig = toolsConfigRepository.findById(toolsConfigId)
                .orElseThrow(() -> new ConfigNotFoundException("ToolsConfig", "toolsConfigId", toolsConfigId));

        if (!existingConfig.getToolName().equals(request.getToolName()) 
                && toolsConfigRepository.existsByToolName(request.getToolName())) {
            throw new ConfigValidationException("Tools config with name '" + request.getToolName() + "' already exists");
        }

        if (request.getClientConfigId() != null && !request.getClientConfigId().equals(existingConfig.getClientConfig().getClientConfigId())) {
            ClientConfig clientConfig = clientConfigRepository.findById(request.getClientConfigId())
                    .orElseThrow(() -> new ConfigNotFoundException("ClientConfig", "clientConfigId", request.getClientConfigId()));
            existingConfig.setClientConfig(clientConfig);
        }

        modelMapper.map(request, existingConfig);

        ToolsConfig updatedConfig = toolsConfigRepository.save(existingConfig);
        log.info("Tools config updated successfully with ID: {}", updatedConfig.getToolsConfigId());

        return ResponseEntity.ok(modelMapper.map(updatedConfig, ToolsConfigResponse.class));
    }

    @Override
    public ResponseEntity<?> deactivateToolsConfig(Long toolsConfigId) {
        log.info("Deactivating tools config with ID: {}", toolsConfigId);
        
        CommonUtils.requireNonNull(toolsConfigId, "Tools config ID");

        ToolsConfig existingConfig = toolsConfigRepository.findById(toolsConfigId)
                .orElseThrow(() -> new ConfigNotFoundException("ToolsConfig", "toolsConfigId", toolsConfigId));

        existingConfig.setIsActive(false);
        toolsConfigRepository.save(existingConfig);
        log.info("Tools config deactivated successfully with ID: {}", toolsConfigId);

        return ResponseEntity.ok(CommonUtils.mapOf("message", "Tools config deactivated successfully"));
    }
}