package com.nexus.nexusbuddy.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexus.nexusbuddy.service.implementations.RecruitmentApiSeederService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for seeding RecruitmentController APIs into NexusBuddy.
 * Provides endpoints to populate tools and params tables from JSON
 * configuration.
 * Supports both default configuration (from classpath) and custom JSON input.
 */
@RestController
@RequestMapping("/nexusbuddy/admin/seeder")
@RequiredArgsConstructor
@Slf4j
public class RecruitmentApiSeederController {

    private final RecruitmentApiSeederService recruitmentApiSeederService;

    /**
     * Seed all GET APIs from RecruitmentController into NexusBuddy tools and params
     * tables
     * using the default JSON configuration from classpath.
     * 
     * POST /nexusbuddy/admin/seeder/recruitment-apis
     * 
     * @return Summary of seeded data
     */
    @PostMapping("/recruitment-apis")
    public ResponseEntity<?> seedRecruitmentApis() {
        log.info("Triggering RecruitmentController GET API seeding with default configuration...");

        try {
            Map<String, Object> result = recruitmentApiSeederService.seedRecruitmentApis();
            log.info("Seeding completed successfully: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Seeding failed: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Seeding failed", "message", e.getMessage()));
        }
    }

    /**
     * Seed recruitment APIs using custom JSON configuration provided in request
     * body.
     * Allows dynamic seeding with custom tool/param definitions.
     * 
     * POST /nexusbuddy/admin/seeder/recruitment-apis/custom
     * 
     * @param customJson Custom JSON configuration with tools array
     * @return Summary of seeded data
     */
    @PostMapping("/recruitment-apis/custom")
    public ResponseEntity<?> seedRecruitmentApisCustom(@RequestBody JsonNode customJson) {
        log.info("Triggering RecruitmentController GET API seeding with custom configuration...");

        try {
            Map<String, Object> result = recruitmentApiSeederService.seedRecruitmentApis(customJson);
            log.info("Custom seeding completed successfully: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Custom seeding failed: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Custom seeding failed", "message", e.getMessage()));
        }
    }

    /**
     * Check if recruitment APIs have already been seeded.
     * 
     * GET /nexusbuddy/admin/seeder/recruitment-apis/status
     * 
     * @return Status of seeded tools
     */
    @GetMapping("/recruitment-apis/status")
    public ResponseEntity<?> getSeedingStatus() {
        log.info("Checking RecruitmentController API seeding status...");

        return ResponseEntity.ok(Map.of(
                "message",
                "Use POST /nexusbuddy/admin/seeder/recruitment-apis to seed recruitment APIs with default config",
                "customEndpoint",
                "Use POST /nexusbuddy/admin/seeder/recruitment-apis/custom with JSON body for custom config",
                "note", "This is a one-time operation. Re-running will skip already existing tools."));
    }
}