package com.nexus.iam.dto.response;

import java.sql.Timestamp;

/**
 * A single stored user preference. {@code value} is the parsed JSON value
 * (string, number, boolean, map or list).
 */
public record UserPreferenceResponse(
        Long userPreferenceId,
        Long userId,
        String settingName,
        String key,
        Object value,
        Boolean isActive,
        Timestamp createdAt,
        Timestamp updatedAt
) {
}
