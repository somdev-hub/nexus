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
    public ResponseEntity<?> getAllClientConfigs() {
        log.info("Fetching all client configs");
        
        List<ClientConfig> configs = clientConfigRepository.findAll();
        List<ClientConfigResponse> responses = configs.stream()
                .map(config -> modelMapper.map(config, ClientConfigResponse.class))
                .toList();

        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<?> getActiveClientConfigs() {
        log.info("Fetching active client configs");
        
        List<ClientConfig> configs = clientConfigRepository.findByIsActiveTrue();
        List<ClientConfigResponse> responses = configs.stream()
                .map(config -> modelMapper.map(config, ClientConfigResponse.class))
                .toList();

        return ResponseEntity.ok(responses);
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