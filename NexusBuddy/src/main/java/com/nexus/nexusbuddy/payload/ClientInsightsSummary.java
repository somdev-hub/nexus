package com.nexus.nexusbuddy.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientInsightsSummary {
    private Long totalHits;
    private Double successPercentage;
    private Double failurePercentage;
    private Double averageResponseTime; // in milliseconds
}