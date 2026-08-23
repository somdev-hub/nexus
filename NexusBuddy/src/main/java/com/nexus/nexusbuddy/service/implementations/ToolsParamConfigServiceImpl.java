package com.nexus.nexusbuddy.service.implementations;

import com.nexus.nexusbuddy.exception.ConfigNotFoundException;
import com.nexus.nexusbuddy.exception.ConfigValidationException;
import com.nexus.nexusbuddy.model.entities.ToolsConfig;
import com.nexus.nexusbuddy.model.entities.ToolsParamConfig;
import com.nexus.nexusbuddy.payload.ToolsParamConfigRequest;
import com.nexus.nexusbuddy.payload.ToolsParamConfigResponse;
import com.nexus.nexusbuddy.repository.ToolsConfigRepository;
import com.nexus.nexusbuddy.repository.ToolsParamConfigRepository;
import com.nexus.nexusbuddy.service.interfaces.ToolsParamConfigService;
import com.nexus.nexusbuddy.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service implementation for Tools Param Config management.
 * Provides CRUD operations for tool parameter configurations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ToolsParamConfigServiceImpl implements ToolsParamConfigService {

    private final ToolsParamConfigRepository toolsParamConfigRepository;
    private final ToolsConfigRepository toolsConfigRepository;
    private final ModelMapper modelMapper;

    @Override
    public ResponseEntity<?> createToolsParamConfig(ToolsParamConfigRequest request) {
        log.info("Creating new tools param config: {}", request.getParamName());
        
        CommonUtils.requireNonNull(request, "Tools param config request");
        CommonUtils.requireNonEmpty(request.getParamName(), "Parameter name");
        CommonUtils.requireNonNull(request.getParamType(), "Parameter type");
        CommonUtils.requireNonNull(request.getDataType(), "Data type");
        CommonUtils.requireNonNull(request.getIsRequired(), "Required flag");
        CommonUtils.requireNonNull(request.getIsActive(), "Active status");
        CommonUtils.requireNonNull(request.getToolsConfigId(), "Tools config ID");

        ToolsConfig toolsConfig = toolsConfigRepository.findById(request.getToolsConfigId())
                .orElseThrow(() -> new ConfigNotFoundException("ToolsConfig", "toolsConfigId", request.getToolsConfigId()));

        if (toolsParamConfigRepository.existsByParamNameAndToolsConfigToolsConfigId(request.getParamName(), request.getToolsConfigId())) {
            throw new ConfigValidationException("Parameter with name '" + request.getParamName() + "' already exists for this tool config");
        }

        ToolsParamConfig paramConfig = modelMapper.map(request, ToolsParamConfig.class);
        paramConfig.setToolsConfig(toolsConfig);

        ToolsParamConfig savedConfig = toolsParamConfigRepository.save(paramConfig);
        log.info("Tools param config created successfully with ID: {}", savedConfig.getToolsParamConfigId());

        return ResponseEntity.status(201).body(modelMapper.map(savedConfig, ToolsParamConfigResponse.class));
    }

    @Override
    public ResponseEntity<?> getToolsParamConfigById(Long toolsParamConfigId) {
        log.info("Fetching tools param config with ID: {}", toolsParamConfigId);
        
        CommonUtils.requireNonNull(toolsParamConfigId, "Tools param config ID");

        ToolsParamConfig paramConfig = toolsParamConfigRepository.findById(toolsParamConfigId)
                .orElseThrow(() -> new ConfigNotFoundException("ToolsParamConfig", "toolsParamConfigId", toolsParamConfigId));

        return ResponseEntity.ok(modelMapper.map(paramConfig, ToolsParamConfigResponse.class));
    }

    @Override
    public ResponseEntity<Page<ToolsParamConfigResponse>> getAllToolsParamConfigs(int page, int size, String sortBy, String sortDir) {
        log.info("Fetching all tools param configs with pagination: page={}, size={}, sortBy={}, sortDir={}", page, size, sortBy, sortDir);
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<ToolsParamConfig> configPage = toolsParamConfigRepository.findAll(pageable);
        
        Page<ToolsParamConfigResponse> responsePage = configPage.map(config -> modelMapper.map(config, ToolsParamConfigResponse.class));
        
        return ResponseEntity.ok(responsePage);
    }

    @Override
    public ResponseEntity<Page<ToolsParamConfigResponse>> getActiveToolsParamConfigs(int page, int size, String sortBy, String sortDir) {
        log.info("Fetching active tools param configs with pagination: page={}, size={}, sortBy={}, sortDir={}", page, size, sortBy, sortDir);
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<ToolsParamConfig> configPage = toolsParamConfigRepository.findByIsActiveTrue(pageable);
        
        Page<ToolsParamConfigResponse> responsePage = configPage.map(config -> modelMapper.map(config, ToolsParamConfigResponse.class));
        
        return ResponseEntity.ok(responsePage);
    }

    @Override
    public ResponseEntity<Page<ToolsParamConfigResponse>> getToolsParamConfigsByToolsConfigId(Long toolsConfigId, int page, int size, String sortBy, String sortDir) {
        log.info("Fetching tools param configs for tools config ID: {} with pagination: page={}, size={}, sortBy={}, sortDir={}", toolsConfigId, page, size, sortBy, sortDir);
        
        CommonUtils.requireNonNull(toolsConfigId, "Tools config ID");

        if (!toolsConfigRepository.existsById(toolsConfigId)) {
            throw new ConfigNotFoundException("ToolsConfig", "toolsConfigId", toolsConfigId);
        }

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<ToolsParamConfig> configPage = toolsParamConfigRepository.findByToolsConfigToolsConfigId(toolsConfigId, pageable);
        
        Page<ToolsParamConfigResponse> responsePage = configPage.map(config -> modelMapper.map(config, ToolsParamConfigResponse.class));
        
        return ResponseEntity.ok(responsePage);
    }

    @Override
    public ResponseEntity<?> updateToolsParamConfig(Long toolsParamConfigId, ToolsParamConfigRequest request) {
        log.info("Updating tools param config with ID: {}", toolsParamConfigId);
        
        CommonUtils.requireNonNull(toolsParamConfigId, "Tools param config ID");
        CommonUtils.requireNonNull(request, "Tools param config request");

        ToolsParamConfig existingConfig = toolsParamConfigRepository.findById(toolsParamConfigId)
                .orElseThrow(() -> new ConfigNotFoundException("ToolsParamConfig", "toolsParamConfigId", toolsParamConfigId));

        if (!existingConfig.getParamName().equals(request.getParamName()) 
                && toolsParamConfigRepository.existsByParamNameAndToolsConfigToolsConfigId(request.getParamName(), existingConfig.getToolsConfig().getToolsConfigId())) {
            throw new ConfigValidationException("Parameter with name '" + request.getParamName() + "' already exists for this tool config");
        }

        if (request.getToolsConfigId() != null && !request.getToolsConfigId().equals(existingConfig.getToolsConfig().getToolsConfigId())) {
            ToolsConfig toolsConfig = toolsConfigRepository.findById(request.getToolsConfigId())
                    .orElseThrow(() -> new ConfigNotFoundException("ToolsConfig", "toolsConfigId", request.getToolsConfigId()));
            existingConfig.setToolsConfig(toolsConfig);
        }

        modelMapper.map(request, existingConfig);

        ToolsParamConfig updatedConfig = toolsParamConfigRepository.save(existingConfig);
        log.info("Tools param config updated successfully with ID: {}", updatedConfig.getToolsParamConfigId());

        return ResponseEntity.ok(modelMapper.map(updatedConfig, ToolsParamConfigResponse.class));
    }

    @Override
    public ResponseEntity<?> deactivateToolsParamConfig(Long toolsParamConfigId) {
        log.info("Deactivating tools param config with ID: {}", toolsParamConfigId);
        
        CommonUtils.requireNonNull(toolsParamConfigId, "Tools param config ID");

        ToolsParamConfig existingConfig = toolsParamConfigRepository.findById(toolsParamConfigId)
                .orElseThrow(() -> new ConfigNotFoundException("ToolsParamConfig", "toolsParamConfigId", toolsParamConfigId));

        existingConfig.setIsActive(false);
        toolsParamConfigRepository.save(existingConfig);
        log.info("Tools param config deactivated successfully with ID: {}", toolsParamConfigId);

        return ResponseEntity.ok(CommonUtils.mapOf("message", "Tools param config deactivated successfully"));
    }
}