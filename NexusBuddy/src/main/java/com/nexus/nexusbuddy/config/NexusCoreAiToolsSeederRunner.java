package com.nexus.nexusbuddy.config;

import com.nexus.nexusbuddy.service.implementations.NexusCoreAiToolsSeederService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Application runner that seeds Nexus Core AI Tools on startup.
 * Idempotent - skips tools that already exist.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NexusCoreAiToolsSeederRunner implements ApplicationRunner {

    private final NexusCoreAiToolsSeederService nexusCoreAiToolsSeederService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Starting Nexus Core AI Tools auto-seeding on startup...");
        try {
            var result = nexusCoreAiToolsSeederService.seedNexusCoreAiTools();
            log.info("Nexus Core AI Tools auto-seeding completed: {}", result);
        } catch (Exception e) {
            log.error("Nexus Core AI Tools auto-seeding failed: {}", e.getMessage(), e);
            // Don't fail startup if seeding fails
        }
    }
}