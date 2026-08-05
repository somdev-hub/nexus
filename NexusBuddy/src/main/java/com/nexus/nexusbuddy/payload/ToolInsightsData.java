package com.nexus.nexusbuddy.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolInsightsData {
    private String toolName;
    private Long totalHits;
    private Long successHits;
    private Long failureHits;
    private String mostFailureCode;
    private Double successRatio;
}