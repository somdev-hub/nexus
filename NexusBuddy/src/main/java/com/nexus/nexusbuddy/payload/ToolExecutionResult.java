package com.nexus.nexusbuddy.payload;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of executing a configured tool.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolExecutionResult {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Name of the tool that was executed.
     */
    private String toolName;

    /**
     * Parameters that were sent to the tool.
     */
    private Object parameters;

    /**
     * Raw response body returned by the tool.
     */
    private Object response;

    /**
     * HTTP status code returned by the tool.
     */
    private Integer statusCode;

    /**
     * Whether the tool execution was successful.
     */
    private boolean success;

    /**
     * Error message if execution failed.
     */
    private String errorMessage;

    public String toPromptString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Tool execution summary").append(System.lineSeparator());
        builder.append("Tool: ").append(toolName != null ? toolName : "unknown").append(System.lineSeparator());
        builder.append("Success: ").append(success).append(System.lineSeparator());

        if (statusCode != null) {
            builder.append("Status: ").append(statusCode).append(System.lineSeparator());
        }

        builder.append("Parameters: ").append(serializeValue(parameters)).append(System.lineSeparator());

        if (success) {
            builder.append("Response: ").append(serializeValue(response)).append(System.lineSeparator());
        } else {
            builder.append("Error: ").append(errorMessage != null ? errorMessage : "Unknown error")
                    .append(System.lineSeparator());
            if (response != null) {
                builder.append("Response: ").append(serializeValue(response)).append(System.lineSeparator());
            }
        }

        return builder.toString();
    }

    private String serializeValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String stringValue) {
            return stringValue;
        }
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }
}
