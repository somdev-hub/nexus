package com.nexus.nexusbuddy.service.implementations;

import com.nexus.nexusbuddy.exception.ConfigNotFoundException;
import com.nexus.nexusbuddy.exception.ConfigValidationException;
import com.nexus.nexusbuddy.model.entities.ClientConfig;
import com.nexus.nexusbuddy.model.entities.ClientConfigAllowedUser;
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
import java.util.stream.Collectors;

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
            throw new ConfigValidationException(
                    "Client config with name '" + request.getClientName() + "' already exists");
        }

        ClientConfig clientConfig = modelMapper.map(request, ClientConfig.class);

        // Handle allowed users list
        if (request.getAllowedUsersList() != null && !request.getAllowedUsersList().isEmpty()) {
            List<ClientConfigAllowedUser> allowedUsers = request.getAllowedUsersList().stream()
                    .map(domain -> ClientConfigAllowedUser.builder()
                            .clientConfig(clientConfig)
                            .domain(domain)
                            .build())
                    .collect(Collectors.toList());
            clientConfig.setAllowedUsers(allowedUsers);
        }

        ClientConfig savedConfig = clientConfigRepository.save(clientConfig);
        log.info("Client config created successfully with ID: {}", savedConfig.getClientConfigId());

        return ResponseEntity.status(201).body(mapToResponse(savedConfig));
    }

    @Override
    public ResponseEntity<?> getClientConfigById(Long clientConfigId) {
        log.info("Fetching client config with ID: {}", clientConfigId);

        CommonUtils.requireNonNull(clientConfigId, "Client config ID");

        ClientConfig clientConfig = clientConfigRepository.findById(clientConfigId)
                .orElseThrow(() -> new ConfigNotFoundException("ClientConfig", "clientConfigId", clientConfigId));

        return ResponseEntity.ok(mapToResponse(clientConfig));
    }

    @Override
    public ResponseEntity<Page<ClientConfigResponse>> getAllClientConfigs(int page, int size, String sortBy,
            String sortDir) {
        log.info("Fetching all client configs with pagination: page={}, size={}, sortBy={}, sortDir={}", page, size,
                sortBy, sortDir);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ClientConfig> configPage = clientConfigRepository.findAll(pageable);

        Page<ClientConfigResponse> responsePage = configPage.map(this::mapToResponse);

        return ResponseEntity.ok(responsePage);
    }

    @Override
    public ResponseEntity<Page<ClientConfigResponse>> getActiveClientConfigs(int page, int size, String sortBy,
            String sortDir) {
        log.info("Fetching active client configs with pagination: page={}, size={}, sortBy={}, sortDir={}", page, size,
                sortBy, sortDir);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ClientConfig> configPage = clientConfigRepository.findByIsActiveTrue(pageable);

        Page<ClientConfigResponse> responsePage = configPage.map(this::mapToResponse);

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
            throw new ConfigValidationException(
                    "Client config with name '" + request.getClientName() + "' already exists");
        }

        // Update basic fields
        existingConfig.setClientName(request.getClientName());
        existingConfig.setConnectionUrl(request.getConnectionUrl());
        existingConfig.setHealthCheckPath(request.getHealthCheckPath());
        existingConfig.setIsActive(request.getIsActive());

        // Update allowed users list
        if (request.getAllowedUsersList() != null) {
            // Clear existing allowed users
            existingConfig.getAllowedUsers().clear();

            // Add new allowed users
            List<ClientConfigAllowedUser> allowedUsers = request.getAllowedUsersList().stream()
                    .map(domain -> ClientConfigAllowedUser.builder()
                            .clientConfig(existingConfig)
                            .domain(domain)
                            .build())
                    .collect(Collectors.toList());
            existingConfig.getAllowedUsers().addAll(allowedUsers);
        }

        ClientConfig updatedConfig = clientConfigRepository.save(existingConfig);
        log.info("Client config updated successfully with ID: {}", updatedConfig.getClientConfigId());

        return ResponseEntity.ok(mapToResponse(updatedConfig));
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

    @Override
    public List<ClientConfig> findByAllowedUsersListContaining(String domain) {
        log.info("Finding client configs with allowed users list containing: {}", domain);
        CommonUtils.requireNonEmpty(domain, "Domain");
        return clientConfigRepository.findByAllowedUsersListContaining(domain);
    }

    private ClientConfigResponse mapToResponse(ClientConfig config) {
        ClientConfigResponse response = modelMapper.map(config, ClientConfigResponse.class);
        if (config.getAllowedUsers() != null) {
            response.setAllowedUsersList(config.getAllowedUsers().stream()
                    .filter(ClientConfigAllowedUser::getIsActive)
                    .map(ClientConfigAllowedUser::getDomain)
                    .collect(Collectors.toList()));
        }
        return response;
    }
}