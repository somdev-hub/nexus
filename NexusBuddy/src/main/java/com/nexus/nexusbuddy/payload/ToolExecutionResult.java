package com.nexus.nexusbuddy.payload;

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
}
