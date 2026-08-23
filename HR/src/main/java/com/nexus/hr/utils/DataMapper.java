package com.nexus.hr.utils;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom DataMapper utility for converting Map objects to DTOs
 * Handles automatic date/time conversions for various formats
 * Supports LocalDateTime, java.sql.Date, and Timestamp conversions
 */
public class DataMapper {

    private static final List<DateTimeFormatter> DATETIME_FORMATTERS = List.of(
            DateTimeFormatter.ISO_DATE_TIME,              // 2024-01-15T10:30:00
            DateTimeFormatter.ISO_DATE_TIME.withZone(java.time.ZoneId.systemDefault()), // with timezone
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"), // ISO with milliseconds
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME        // 2024-01-15T10:30:00
    );

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,             // 2024-01-15
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    );

    /**
     * Convert a Map to a target DTO class
     * Automatically handles type conversions including dates and times
     *
     * @param source Source map with data
     * @param targetClass Target DTO class
     * @param <T> Generic type parameter
     * @return Instance of target class with mapped data
     */
    public static <T> T mapToObject(Map<String, Object> source, Class<T> targetClass) {
        if (source == null || source.isEmpty()) {
            return null;
        }

        try {
            T target = targetClass.getDeclaredConstructor().newInstance();

            for (var field : targetClass.getDeclaredFields()) {
                field.setAccessible(true);
                String fieldName = field.getName();
                Object value = source.get(fieldName);

                if (value != null) {
                    Object convertedValue = convertValue(value, field.getType());
                    field.set(target, convertedValue);
                }
            }

            return target;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map to " + targetClass.getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Convert a List of Maps to a List of target DTO objects
     *
     * @param sources List of source maps
     * @param targetClass Target DTO class
     * @param <T> Generic type parameter
     * @return List of target objects
     */
    public static <T> List<T> mapToObjectList(List<Map<String, Object>> sources, Class<T> targetClass) {
        if (sources == null || sources.isEmpty()) {
            return new ArrayList<>();
        }

        return sources.stream()
                .map(source -> mapToObject(source, targetClass))
                .toList();
    }

    /**
     * Convert value to target type with support for date/time conversions
     *
     * @param value Source value
     * @param targetType Target type class
     * @return Converted value
     */
    @SuppressWarnings("unchecked")
    private static Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        // If already the correct type, return as-is
        if (targetType.isInstance(value)) {
            return value;
        }

        // String conversions
        if (targetType == String.class) {
            return value.toString();
        }

        // Long conversions
        if (targetType == Long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.parseLong(value.toString());
        }

        // Double conversions
        if (targetType == Double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.parseDouble(value.toString());
        }

        // Integer conversions
        if (targetType == Integer.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        }

        // Boolean conversions
        if (targetType == Boolean.class) {
            if (value instanceof Boolean) {
                return value;
            }
            String strValue = value.toString().toLowerCase();
            return strValue.equals("true") || strValue.equals("1") || strValue.equals("yes");
        }

        // LocalDateTime conversions
        if (targetType == LocalDateTime.class) {
            return convertToLocalDateTime(value);
        }

        // java.sql.Date conversions
        if (targetType == Date.class) {
            return convertToSqlDate(value);
        }

        // Timestamp conversions
        if (targetType == Timestamp.class) {
            return convertToTimestamp(value);
        }

        // List conversions
        if (targetType == List.class && value instanceof List) {
            return value;
        }

        // Map conversions
        if (targetType == Map.class && value instanceof Map) {
            return value;
        }

        // Default: return as string if target is String, otherwise return original value
        return value;
    }

    /**
     * Convert value to LocalDateTime
     * Handles multiple date/time formats
     */
    private static LocalDateTime convertToLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }

        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }

        if (value instanceof Date) {
            return LocalDateTime.of(((Date) value).toLocalDate(), LocalDateTime.MIN.toLocalTime());
        }

        String stringValue = value.toString().trim();

        // Try each formatter
        for (DateTimeFormatter formatter : DATETIME_FORMATTERS) {
            try {
                // Remove 'Z' suffix if present
                if (stringValue.endsWith("Z")) {
                    stringValue = stringValue.substring(0, stringValue.length() - 1);
                }
                return LocalDateTime.parse(stringValue, formatter);
            } catch (Exception e) {
                // Try next formatter
            }
        }

        // If no datetime format worked, try date-only formats
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                java.time.LocalDate date = java.time.LocalDate.parse(stringValue, formatter);
                return LocalDateTime.of(date, LocalDateTime.MIN.toLocalTime());
            } catch (Exception e) {
                // Try next formatter
            }
        }

        throw new RuntimeException("Unable to convert '" + value + "' to LocalDateTime");
    }

    /**
     * Convert value to java.sql.Date
     * Handles multiple date formats
     */
    private static Date convertToSqlDate(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Date) {
            return (Date) value;
        }

        if (value instanceof Timestamp) {
            return new Date(((Timestamp) value).getTime());
        }

        if (value instanceof LocalDateTime) {
            return Date.valueOf(((LocalDateTime) value).toLocalDate());
        }

        String stringValue = value.toString().trim();

        // Extract date part if it contains time
        if (stringValue.contains("T")) {
            stringValue = stringValue.substring(0, stringValue.indexOf("T"));
        }

        try {
            return Date.valueOf(stringValue);
        } catch (Exception e) {
            throw new RuntimeException("Unable to convert '" + value + "' to java.sql.Date", e);
        }
    }

    /**
     * Convert value to java.sql.Timestamp
     * Handles multiple timestamp formats
     */
    private static Timestamp convertToTimestamp(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Timestamp) {
            return (Timestamp) value;
        }

        if (value instanceof LocalDateTime) {
            return Timestamp.valueOf((LocalDateTime) value);
        }

        if (value instanceof Date) {
            return new Timestamp(((Date) value).getTime());
        }

        String stringValue = value.toString().trim();

        // Remove 'Z' suffix if present
        if (stringValue.endsWith("Z")) {
            stringValue = stringValue.substring(0, stringValue.length() - 1);
        }

        // Replace 'T' with space for standard SQL format
        if (stringValue.contains("T")) {
            stringValue = stringValue.replace("T", " ");
        }

        try {
            return Timestamp.valueOf(stringValue);
        } catch (Exception e) {
            // Try parsing as LocalDateTime first
            try {
                LocalDateTime ldt = convertToLocalDateTime(stringValue);
                return Timestamp.valueOf(ldt);
            } catch (Exception ex) {
                throw new RuntimeException("Unable to convert '" + value + "' to java.sql.Timestamp", ex);
            }
        }
    }

    /**
     * Extract a specific field from a map and convert to target type
     *
     * @param map Source map
     * @param key Field key
     * @param targetType Target type
     * @param <T> Generic type parameter
     * @return Converted value or null if key not found
     */
    @SuppressWarnings("unchecked")
    public static <T> T extractField(Map<String, Object> map, String key, Class<T> targetType) {
        if (map == null) {
            return null;
        }

        Object value = map.get(key);
        if (value == null) {
            return null;
        }

        return (T) convertValue(value, targetType);
    }

    /**
     * Extract a list field from a map and convert each element
     *
     * @param map Source map
     * @param key Field key
     * @param elementType Target element type
     * @param <T> Generic type parameter
     * @return List of converted elements or empty list if key not found
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> extractListField(Map<String, Object> map, String key, Class<T> elementType) {
        if (map == null) {
            return new ArrayList<>();
        }

        Object value = map.get(key);
        if (!(value instanceof List)) {
            return new ArrayList<>();
        }

        List<Object> sourceList = (List<Object>) value;
        if (sourceList.isEmpty()) {
            return new ArrayList<>();
        }

        // If elements are maps, convert to objects
        if (sourceList.get(0) instanceof Map) {
            return sourceList.stream()
                    .map(item -> mapToObject((Map<String, Object>) item, elementType))
                    .toList();
        }

        // Otherwise convert each element directly
        return sourceList.stream()
                .map(item -> (T) convertValue(item, elementType))
                .toList();
    }

    /**
     * Extract a list field from a map (for lists of Maps only)
     * Returns List<Map<String, Object>> directly without type conversion
     *
     * @param map Source map
     * @param key Field key
     * @return List of Maps or empty list if key not found
     */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> extractListOfMaps(Map<String, Object> map, String key) {
        if (map == null) {
            return new ArrayList<>();
        }

        Object value = map.get(key);
        if (!(value instanceof List)) {
            return new ArrayList<>();
        }

        return (List<Map<String, Object>>) value;
    }

    /**
     * Extract a nested map from a map
     *
     * @param map Source map
     * @param key Field key
     * @return Nested map or empty map if key not found
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> extractMapField(Map<String, Object> map, String key) {
        if (map == null) {
            return new HashMap<>();
        }

        Object value = map.get(key);
        if (!(value instanceof Map)) {
            return new HashMap<>();
        }

        return (Map<String, Object>) value;
    }
}



