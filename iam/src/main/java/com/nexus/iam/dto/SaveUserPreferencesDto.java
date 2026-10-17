package com.nexus.iam.dto;

import java.util.List;

/**
 * Payload for saving a user's preferences for a single setting group.
 *
 * <p>
 * The class name ends with {@code Dto} so {@code @LogActivity} captures the
 * request body for the activity log.
 * </p>
 *
 * <p>
 * {@code value} is intentionally a plain {@code Object} (not a Jackson
 * {@code JsonNode}): the HTTP layer runs on Jackson 3 ({@code tools.jackson})
 * in Spring Boot 4, while this module also has Jackson 2 on the classpath, so
 * a Jackson 2 {@code JsonNode} cannot be bound by the message converter.
 * Jackson binds untyped JSON to {@code String} / {@code Number} /
 * {@code Boolean} / {@code Map} / {@code List}, which is then serialized back
 * to JSON text for the {@code jsonb} column.
 * </p>
 */
public record SaveUserPreferencesDto(
        String settingName,
        List<Preference> preferences
) {

    /**
     * A single preference entry. {@code value} is an arbitrary JSON value and is
     * persisted into the {@code jsonb} column.
     */
    public record Preference(
            String key,
            Object value
    ) {
    }
}
