package com.nexus.hr.service.implementations;

import com.nexus.hr.exception.ResourceNotFoundException;
import com.nexus.hr.exception.ServiceLevelException;
import com.nexus.hr.model.entities.WOWOConfig;
import com.nexus.hr.repository.WOWOConfigRepo;
import com.nexus.hr.service.interfaces.WOWOConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
@RequiredArgsConstructor
public class WOWOConfigServiceImpl implements WOWOConfigService {

    private final WOWOConfigRepo wowoConfigRepo;

    @Override
    public ResponseEntity<?> addWOWOConfig(WOWOConfig wOWOConfig) {
        try {
            WOWOConfig save = wowoConfigRepo.save(wOWOConfig);
            return ResponseEntity.ok().body(save);
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "WOWOConfigService",
                    "Failed to add wowo config",
                    "addWOWOConfig",
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> toggleWOWOConfig(Long configId) {
        if (ObjectUtils.isEmpty(configId)) {
            throw new ServiceLevelException(
                    "WOWOConfigService",
                    "Config ID cannot be null or empty",
                    "toggleWOWOConfig",
                    "InvalidInputException",
                    "Config ID is required"
            );
        }
        try {
            WOWOConfig wowoConfig = wowoConfigRepo.findById(configId).orElseThrow(() -> new ResourceNotFoundException("WOWOConfig", "configId", configId));
            wowoConfig.setIsActive(!wowoConfig.getIsActive());
            WOWOConfig save = wowoConfigRepo.save(wowoConfig);
            return ResponseEntity.ok().body(save);
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "WOWOConfigService",
                    "Failed to toggle wowo config",
                    "toggleWOWOConfig",
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> getWOWOConfig(Long configId) {
        if (ObjectUtils.isEmpty(configId)) {
            throw new ServiceLevelException(
                    "WOWOConfigService",
                    "Config ID cannot be null or empty",
                    "getWOWOConfig",
                    "InvalidInputException",
                    "Config ID is required"
            );
        }
        try {
            WOWOConfig wowoConfig = wowoConfigRepo.findById(configId).orElseThrow(() -> new ResourceNotFoundException("WOWOConfig", "configId", configId));
            return ResponseEntity.ok().body(wowoConfig);
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "WOWOConfigService",
                    "Failed to get wowo config",
                    "getWOWOConfig",
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<WOWOConfig> getWOWOConfigByName(String wowoName) {
        if (ObjectUtils.isEmpty(wowoName)) {
            throw new ServiceLevelException(
                    "WOWOConfigService",
                    "WOWO name cannot be null or empty",
                    "getWOWOConfigByName",
                    "InvalidInputException",
                    "WOWO name is required"
            );
        }
        try {
            WOWOConfig wowoConfig = wowoConfigRepo.findByWowoName(wowoName).orElseThrow(() -> new ResourceNotFoundException("WOWOConfig", "wowoName", wowoName));
            return ResponseEntity.ok().body(wowoConfig);
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "WOWOConfigService",
                    "Failed to get wowo config by name",
                    "getWOWOConfigByName",
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
        }
    }
}
