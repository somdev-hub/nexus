package com.nexus.iam.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.iam.dto.SaveUserPreferencesDto;
import com.nexus.iam.dto.response.UserPreferenceResponse;
import com.nexus.iam.entities.UserPreference;
import com.nexus.iam.repository.UserPreferenceRepository;
import com.nexus.iam.service.UserPreferenceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getPreferences(Long userId, String settingName) {
        if (ObjectUtils.isEmpty(userId)) {
            return ResponseEntity.badRequest().body("userId is required");
        }
        if (ObjectUtils.isEmpty(settingName)) {
            return ResponseEntity.badRequest().body("settingName is required");
        }

        Map<String, Object> preferences = new LinkedHashMap<>();
        for (UserPreference preference : userPreferenceRepository
                .findByUserIdAndSettingNameAndIsActiveTrue(userId, settingName)) {
            preferences.put(preference.getKey(), toJsonValue(preference.getValue()));
        }
        return ResponseEntity.ok(preferences);
    }

    @Override
    @Transactional
    public ResponseEntity<?> savePreferences(Long userId, SaveUserPreferencesDto request) {
        if (ObjectUtils.isEmpty(userId)) {
            return ResponseEntity.badRequest().body("userId is required");
        }
        if (request == null || ObjectUtils.isEmpty(request.settingName())) {
            return ResponseEntity.badRequest().body("settingName is required");
        }
        if (ObjectUtils.isEmpty(request.preferences())) {
            return ResponseEntity.badRequest().body("preferences are required");
        }

        List<String> keys = request.preferences().stream()
                .filter(item -> item != null && !ObjectUtils.isEmpty(item.key()))
                .map(SaveUserPreferencesDto.Preference::key)
                .toList();

        // Load the existing rows for this user/setting in one query, then upsert.
        Map<String, UserPreference> existing = new LinkedHashMap<>();
        if (!keys.isEmpty()) {
            userPreferenceRepository
                    .findByUserIdAndSettingNameAndKeyIn(userId, request.settingName(), keys)
                    .forEach(preference -> existing.put(preference.getKey(), preference));
        }

        List<UserPreferenceResponse> saved = new ArrayList<>();
        for (SaveUserPreferencesDto.Preference item : request.preferences()) {
            if (item == null || ObjectUtils.isEmpty(item.key())) {
                continue;
            }
            UserPreference preference = existing.getOrDefault(item.key(), new UserPreference());
            preference.setUserId(userId);
            preference.setSettingName(request.settingName());
            preference.setKey(item.key());
            preference.setValue(toJsonText(item.value()));
            preference.setIsActive(true);
            saved.add(toResponse(userPreferenceRepository.save(preference)));
        }
        return ResponseEntity.ok(saved);
    }

    /**
     * Parses the JSON text stored in the {@code jsonb} column back into a plain
     * Java value (String / Number / Boolean / Map / List).
     */
    private Object toJsonValue(String json) {
        if (ObjectUtils.isEmpty(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            log.warn("Stored preference value is not valid JSON, returning it as text: {}", e.getMessage());
            return json;
        }
    }

    /** Serializes an arbitrary preference value into JSON text for the {@code jsonb} column. */
    private String toJsonText(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Could not serialize preference value of type {}: {}",
                    value.getClass().getName(), e.getMessage());
            return null;
        }
    }

    private UserPreferenceResponse toResponse(UserPreference preference) {
        return new UserPreferenceResponse(
                preference.getUserPreferenceId(),
                preference.getUserId(),
                preference.getSettingName(),
                preference.getKey(),
                toJsonValue(preference.getValue()),
                preference.getIsActive(),
                preference.getCreatedAt(),
                preference.getUpdatedAt());
    }
}
