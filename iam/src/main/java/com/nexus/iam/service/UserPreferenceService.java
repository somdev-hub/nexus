package com.nexus.iam.service;

import org.springframework.http.ResponseEntity;

import com.nexus.iam.dto.SaveUserPreferencesDto;

public interface UserPreferenceService {

    /**
     * Returns the active preferences for a user within a setting group as a
     * {@code key -> value} map. Keys the user has never saved are simply absent,
     * letting the caller fall back to its own defaults.
     */
    ResponseEntity<?> getPreferences(Long userId, String settingName);

    /** Inserts or updates the given preferences for a user within a setting group. */
    ResponseEntity<?> savePreferences(Long userId, SaveUserPreferencesDto request);
}
