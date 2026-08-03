package com.nexus.nexusbuddy.service.implementations;

import com.nexus.nexusbuddy.exception.ConfigNotFoundException;
import com.nexus.nexusbuddy.exception.ConfigValidationException;
import com.nexus.nexusbuddy.model.entities.ClientConfig;
import com.nexus.nexusbuddy.payload.ClientConfigRequest;
import com.nexus.nexusbuddy.payload.ClientConfigResponse;
import com.nexus.nexusbuddy.repository.ClientConfigRepository;
import com.nexus.nexusbuddy.service.interfaces.ClientConfigService;
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
 * Service implementation for Client Config management.
 * Provides CRUD operations for client configurations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ClientConfigServiceImpl implements ClientConfigService {

    private final ClientConfigRepository clientConfigRepository;
    private final ModelMapper modelMapper;

    @Override
    public ResponseEntity<?> createClientConfig(ClientConfigRequest request) {
        log.info("Creating new client config: {}", request.getClientName());
        
        CommonUtils.requireNonNull(request, "Client config request");
        CommonUtils.requireNonEmpty(request.getClientName(), "Client name");
        CommonUtils.requireNonEmpty(request.getConnectionUrl(), "Connection URL");
        CommonUtils.requireNonNull(request.getIsActive(), "Active status");

        if (clientConfigRepository.existsByClientName(request.getClientName())) {
            throw new ConfigValidationException("Client config with name '" + request.getClientName() + "' already exists");
        }

        ClientConfig clientConfig = modelMapper.map(request, ClientConfig.class);

        ClientConfig savedConfig = clientConfigRepository.save(clientConfig);
        log.info("Client config created successfully with ID: {}", savedConfig.getClientConfigId());

        return ResponseEntity.status(201).body(modelMapper.map(savedConfig, ClientConfigResponse.class));
    }

    @Override
    public ResponseEntity<?> getClientConfigById(Long clientConfigId) {
        log.info("Fetching client config with ID: {}", clientConfigId);
        
        CommonUtils.requireNonNull(clientConfigId, "Client config ID");

        ClientConfig clientConfig = clientConfigRepository.findById(clientConfigId)
                .orElseThrow(() -> new ConfigNotFoundException("ClientConfig", "clientConfigId", clientConfigId));

        return ResponseEntity.ok(modelMapper.map(clientConfig, ClientConfigResponse.class));
    }

    @Override
    public ResponseEntity<Page<ClientConfigResponse>> getAllClientConfigs(int page, int size, String sortBy, String sortDir) {
        log.info("Fetching all client configs with pagination: page={}, size={}, sortBy={}, sortDir={}", page, size, sortBy, sortDir);
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<ClientConfig> configPage = clientConfigRepository.findAll(pageable);
        
        Page<ClientConfigResponse> responsePage = configPage.map(config -> modelMapper.map(config, ClientConfigResponse.class));
        
        return ResponseEntity.ok(responsePage);
    }

    @Override
    public ResponseEntity<Page<ClientConfigResponse>> getActiveClientConfigs(int page, int size, String sortBy, String sortDir) {
        log.info("Fetching active client configs with pagination: page={}, size={}, sortBy={}, sortDir={}", page, size, sortBy, sortDir);
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<ClientConfig> configPage = clientConfigRepository.findByIsActiveTrue(pageable);
        
        Page<ClientConfigResponse> responsePage = configPage.map(config -> modelMapper.map(config, ClientConfigResponse.class));
        
        return ResponseEntity.ok(responsePage);
    }

    @Override
    public ResponseEntity<?> updateClientConfig(Long clientConfigId, ClientConfigRequest request) {
        log.info("Updating client config with ID: {}", clientConfigId);
        
        CommonUtils.requireNonNull(clientConfigId, "Client config ID");
        CommonUtils.requireNonNull(request, "Client config request");

        ClientConfig existingConfig = clientConfigRepository.findById(clientConfigId)
                .orElseThrow(() -> new ConfigNotFoundException("ClientConfig", "clientConfigId", clientConfigId));

        if (!existingConfig.getClientName().equals(request.getClientName()) 
                && clientConfigRepository.existsByClientName(request.getClientName())) {
            throw new ConfigValidationException("Client config with name '" + request.getClientName() + "' already exists");
        }

        modelMapper.map(request, existingConfig);

        ClientConfig updatedConfig = clientConfigRepository.save(existingConfig);
        log.info("Client config updated successfully with ID: {}", updatedConfig.getClientConfigId());

        return ResponseEntity.ok(modelMapper.map(updatedConfig, ClientConfigResponse.class));
    }

    @Override
    public ResponseEntity<?> deactivateClientConfig(Long clientConfigId) {
        log.info("Deactivating client config with ID: {}", clientConfigId);
        
        CommonUtils.requireNonNull(clientConfigId, "Client config ID");

        ClientConfig existingConfig = clientConfigRepository.findById(clientConfigId)
                .orElseThrow(() -> new ConfigNotFoundException("ClientConfig", "clientConfigId", clientConfigId));

        existingConfig.setIsActive(false);
        clientConfigRepository.save(existingConfig);
        log.info("Client config deactivated successfully with ID: {}", clientConfigId);

        return ResponseEntity.ok(CommonUtils.mapOf("message", "Client config deactivated successfully"));
    }
}