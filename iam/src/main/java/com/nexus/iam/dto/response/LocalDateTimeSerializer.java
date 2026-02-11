package com.nexus.iam.dto.response;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Custom JSON serializer for LocalDateTime
 * Formats dates as "26 Jan 2026" (localized format)
 */
public class LocalDateTimeSerializer extends ValueSerializer<LocalDateTime> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("dd MMM yyyy", Locale.ENGLISH);

    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext ctxt) {
        try {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeString(value.format(FORMATTER));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error serializing LocalDateTime", e);
        }
    }
}








