package com.nexus.nexusbuddy.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Common utility methods for NexusBuddy.
 * Provides JSON serialization, URI building, map operations, and other common
 * utilities.
 */
public class CommonUtils {

    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private CommonUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a configured ObjectMapper instance.
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        return mapper;
    }

    // ==================== JSON SERIALIZATION ====================

    /**
     * Converts object to JSON string.
     */
    public static String toJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    /**
     * Converts JSON string to object of specified type.
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize from JSON", e);
        }
    }

    /**
     * Converts JSON string to object using TypeReference for generic types.
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize from JSON", e);
        }
    }

    /**
     * Converts object to Map.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(Object object) {
        if (object == null) {
            return Collections.emptyMap();
        }
        if (object instanceof Map) {
            return (Map<String, Object>) object;
        }
        return OBJECT_MAPPER.convertValue(object, new TypeReference<Map<String, Object>>() {
        });
    }

    /**
     * Converts JSON string to Map.
     */
    public static Map<String, Object> jsonToMap(String json) {
        return fromJson(json, new TypeReference<Map<String, Object>>() {
        });
    }

    /**
     * Pretty prints JSON string.
     */
    public static String prettyPrint(String json) {
        try {
            Object obj = OBJECT_MAPPER.readValue(json, Object.class);
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return json; // Return original if not valid JSON
        }
    }

    // ==================== URI BUILDING ====================

    /**
     * Builds URI with query parameters.
     */
    public static String buildUri(String baseUrl, String path, Map<String, Object> queryParams) {
        StringBuilder url = new StringBuilder(baseUrl);

        if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            url.append("/");
        }
        url.append(path);

        if (queryParams != null && !queryParams.isEmpty()) {
            url.append("?");
            boolean first = true;
            for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
                if (!first)
                    url.append("&");
                url.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                url.append("=");
                url.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
                first = false;
            }
        }

        return url.toString();
    }

    /**
     * Builds URI with path variables replaced.
     */
    public static String buildUriWithPathVars(String baseUrl, String pathTemplate, Map<String, String> pathVars,
            Map<String, Object> queryParams) {
        String path = pathTemplate;
        if (pathVars != null) {
            for (Map.Entry<String, String> entry : pathVars.entrySet()) {
                path = path.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return buildUri(baseUrl, path, queryParams);
    }

    // ==================== MAP OPERATIONS ====================

    /**
     * Creates an immutable map from key-value pairs.
     */
    @SafeVarargs
    public static <K, V> Map<K, V> mapOf(Object... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Key-value pairs must be even number");
        }
        Map<K, V> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put((K) keyValuePairs[i], (V) keyValuePairs[i + 1]);
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * Creates a mutable map from key-value pairs.
     */
    @SafeVarargs
    public static <K, V> Map<K, V> mutableMapOf(Object... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Key-value pairs must be even number");
        }
        Map<K, V> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put((K) keyValuePairs[i], (V) keyValuePairs[i + 1]);
        }
        return map;
    }

    /**
     * Merges two maps, second map overrides first.
     */
    public static <K, V> Map<K, V> mergeMaps(Map<K, V> map1, Map<K, V> map2) {
        Map<K, V> result = new LinkedHashMap<>();
        if (map1 != null)
            result.putAll(map1);
        if (map2 != null)
            result.putAll(map2);
        return result;
    }

    /**
     * Filters map entries by predicate.
     */
    public static <K, V> Map<K, V> filterMap(Map<K, V> map, java.util.function.BiPredicate<K, V> predicate) {
        if (map == null)
            return Collections.emptyMap();
        return map.entrySet().stream()
                .filter(entry -> predicate.test(entry.getKey(), entry.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v1,
                        LinkedHashMap::new));
    }

    /**
     * Gets value from nested map using dot notation path.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getNestedValue(Map<String, Object> map, String path, T defaultValue) {
        if (map == null || path == null || path.isEmpty()) {
            return defaultValue;
        }
        String[] keys = path.split("\\.");
        Object current = map;
        for (String key : keys) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(key);
            } else {
                return defaultValue;
            }
        }
        return (T) (current != null ? current : defaultValue);
    }

    /**
     * Sets value in nested map using dot notation path.
     */
    public static void setNestedValue(Map<String, Object> map, String path, Object value) {
        if (map == null || path == null || path.isEmpty()) {
            return;
        }
        String[] keys = path.split("\\.");
        Map<String, Object> current = map;
        for (int i = 0; i < keys.length - 1; i++) {
            String key = keys[i];
            Object next = current.get(key);
            if (next instanceof Map) {
                current = (Map<String, Object>) next;
            } else {
                Map<String, Object> newMap = new LinkedHashMap<>();
                current.put(key, newMap);
                current = newMap;
            }
        }
        current.put(keys[keys.length - 1], value);
    }

    // ==================== COLLECTION UTILITIES ====================

    /**
     * Checks if collection is null or empty.
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Checks if map is null or empty.
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Checks if string is null or empty/whitespace.
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Checks if string is not null and not empty.
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * Returns first non-null value from varargs.
     */
    @SafeVarargs
    public static <T> T firstNonNull(T... values) {
        if (values != null) {
            for (T value : values) {
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Returns default value if input is null.
     */
    public static <T> T defaultIfNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    // ==================== MULTIPART HANDLING ====================

    /**
     * Checks if map contains MultipartFile values.
     */
    public static boolean containsMultipartFile(Map<String, Object> map) {
        if (map == null)
            return false;
        return map.values().stream().anyMatch(v -> v instanceof MultipartFile);
    }

    /**
     * Creates MultiValueMap for multipart requests, handling MultipartFile
     * specially.
     */
    public static MultiValueMap<String, Object> toMultipartBody(Map<String, Object> parts) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        if (parts != null) {
            for (Map.Entry<String, Object> entry : parts.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof MultipartFile file) {
                    try {
                        body.add(entry.getKey(), new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
                            @Override
                            public String getFilename() {
                                return file.getOriginalFilename();
                            }
                        });
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read multipart file: " + file.getOriginalFilename(), e);
                    }
                } else if (value instanceof Collection) {
                    ((Collection<?>) value).forEach(item -> body.add(entry.getKey(), item));
                } else {
                    body.add(entry.getKey(), value);
                }
            }
        }
        return body;
    }

    /**
     * Serializes payload for logging, masking MultipartFile content.
     */
    public static String serializePayloadForLogging(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            if (payload instanceof Map) {
                Map<String, Object> map = new LinkedHashMap<>((Map<String, Object>) payload);
                // Replace MultipartFile with placeholder
                map.replaceAll((k, v) -> v instanceof MultipartFile
                        ? "MultipartFile:" + ((MultipartFile) v).getOriginalFilename()
                        : v);
                return OBJECT_MAPPER.writeValueAsString(map);
            } else {
                return OBJECT_MAPPER.writeValueAsString(payload);
            }
        } catch (JsonProcessingException e) {
            return payload.toString();
        }
    }

    // ==================== TIME & DATE ====================

    /**
     * Returns current timestamp as formatted string.
     */
    public static String now() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }

    /**
     * Returns current timestamp in ISO format.
     */
    public static String nowIso() {
        return LocalDateTime.now().toString();
    }

    /**
     * Formats LocalDateTime to string.
     */
    public static String format(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : null;
    }

    /**
     * Parses string to LocalDateTime.
     */
    public static LocalDateTime parse(String dateTimeStr) {
        if (isEmpty(dateTimeStr))
            return null;
        return LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
    }

    // ==================== VALIDATION ====================

    /**
     * Validates that string is not null or empty.
     */
    public static void requireNonEmpty(String value, String fieldName) {
        if (isEmpty(value)) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
    }

    /**
     * Validates that object is not null.
     */
    public static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }

    /**
     * Validates that collection is not null or empty.
     */
    public static <T> Collection<T> requireNonEmpty(Collection<T> collection, String fieldName) {
        if (isEmpty(collection)) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
        return collection;
    }

    // ==================== STRING UTILITIES ====================

    /**
     * Joins collection elements with delimiter.
     */
    public static String join(Collection<?> collection, String delimiter) {
        if (isEmpty(collection))
            return "";
        return collection.stream()
                .map(Object::toString)
                .collect(Collectors.joining(delimiter));
    }

    /**
     * Truncates string to max length with ellipsis.
     */
    public static String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength)
            return str;
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * Masks sensitive string (shows first and last n chars).
     */
    public static String mask(String str, int visibleChars) {
        if (str == null || str.length() <= visibleChars * 2)
            return "****";
        return str.substring(0, visibleChars) + "****" + str.substring(str.length() - visibleChars);
    }

    /**
     * Generates a random alphanumeric string.
     */
    public static String randomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // ==================== TYPE CONVERSION ====================

    /**
     * Safely converts object to string.
     */
    public static String toString(Object obj) {
        return obj != null ? obj.toString() : null;
    }

    /**
     * Safely converts object to Long.
     */
    public static Long toLong(Object obj) {
        if (obj == null)
            return null;
        if (obj instanceof Number)
            return ((Number) obj).longValue();
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Safely converts object to Integer.
     */
    public static Integer toInteger(Object obj) {
        if (obj == null)
            return null;
        if (obj instanceof Number)
            return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Safely converts object to Boolean.
     */
    public static Boolean toBoolean(Object obj) {
        if (obj == null)
            return null;
        if (obj instanceof Boolean)
            return (Boolean) obj;
        String str = obj.toString().toLowerCase();
        return "true".equals(str) || "1".equals(str) || "yes".equals(str) || "on".equals(str);
    }

    // ==================== EXCEPTION HANDLING ====================

    /**
     * Executes a function and wraps any exception in RuntimeException.
     */
    public static <T> T unchecked(Function<Void, T> function) {
        try {
            return function.apply(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Executes a runnable and wraps any exception in RuntimeException.
     */
    public static void unchecked(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets root cause of throwable.
     */
    public static Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    /**
     * Gets all messages from exception chain.
     */
    public static String getAllMessages(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            if (sb.length() > 0)
                sb.append(" -> ");
            sb.append(current.getMessage());
            current = current.getCause();
        }
        return sb.toString();
    }
}