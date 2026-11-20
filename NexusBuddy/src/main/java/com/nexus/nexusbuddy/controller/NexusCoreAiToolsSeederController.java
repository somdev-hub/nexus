package com.nexus.nexusbuddy.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexus.nexusbuddy.service.implementations.NexusCoreAiToolsSeederService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for seeding Nexus Core AI Tools into NexusBuddy.
 * Provides endpoints to populate tools and params tables from JSON configuration.
 * Supports both default configuration (from classpath) and custom JSON input.
 * Idempotent - skips tools that already exist.
 */
@RestController
@RequestMapping("/nexusbuddy/admin/seeder")
@RequiredArgsConstructor
@Slf4j
public class NexusCoreAiToolsSeederController {

    private final NexusCoreAiToolsSeederService nexusCoreAiToolsSeederService;

    /**
     * Seed Nexus Core AI Tools into NexusBuddy tools and params tables
     * using the default JSON configuration from classpath.
     * POST /nexusbuddy/admin/seeder/nexus-core-ai-tools
     *
     * @return Summary of seeded data
     */
    @PostMapping("/nexus-core-ai-tools")
    public ResponseEntity<?> seedNexusCoreAiTools() {
        log.info("Triggering Nexus Core AI Tools seeding with default configuration...");

        try {
            Map<String, Object> result = nexusCoreAiToolsSeederService.seedNexusCoreAiTools();
            log.info("Seeding completed successfully: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Seeding failed: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Seeding failed", "message", e.getMessage()));
        }
    }

    /**
     * Seed Nexus Core AI Tools using custom JSON configuration provided in request body.
     * POST /nexusbuddy/admin/seeder/nexus-core-ai-tools/custom
     *
     * @param customJson Custom JSON configuration with tools array
     * @return Summary of seeded data
     */
    @PostMapping("/nexus-core-ai-tools/custom")
    public ResponseEntity<?> seedNexusCoreAiToolsCustom(@RequestBody JsonNode customJson) {
        log.info("Triggering Nexus Core AI Tools seeding with custom configuration...");

        try {
            Map<String, Object> result = nexusCoreAiToolsSeederService.seedNexusCoreAiTools(customJson);
            log.info("Custom seeding completed successfully: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Custom seeding failed: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Custom seeding failed", "message", e.getMessage()));
        }
    }

    /**
     * Check if Nexus Core AI Tools have already been seeded.
     * GET /nexusbuddy/admin/seeder/nexus-core-ai-tools/status
     *
     * @return Status of seeded tools
     */
    @GetMapping("/nexus-core-ai-tools/status")
    public ResponseEntity<?> getSeedingStatus() {
        log.info("Checking Nexus Core AI Tools seeding status...");

        return ResponseEntity.ok(Map.of(
                "message",
                "Use POST /nexusbuddy/admin/seeder/nexus-core-ai-tools to seed Nexus Core AI Tools with default config",
                "customEndpoint",
                "Use POST /nexusbuddy/admin/seeder/nexus-core-ai-tools/custom with JSON body for custom config",
                "note", "This is a one-time operation. Re-running will skip already existing tools."));
    }
}