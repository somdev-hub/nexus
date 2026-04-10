package com.nexus.hr.service.interfaces;

import com.nexus.hr.model.entities.WOWOConfig;
import org.springframework.http.ResponseEntity;

public interface WOWOConfigService {

    ResponseEntity<?> addWOWOConfig(WOWOConfig wOWOConfig);

    ResponseEntity<?> toggleWOWOConfig(Long configId);

    ResponseEntity<?> getWOWOConfig(Long configId);

    ResponseEntity<WOWOConfig> getWOWOConfigByName(String wowoName);
}
