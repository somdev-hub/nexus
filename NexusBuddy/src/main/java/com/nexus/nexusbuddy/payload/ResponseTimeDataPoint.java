package com.nexus.nexusbuddy.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseTimeDataPoint {
    private String timestamp;
    private Double avgResponseTime;
    private Double minResponseTime;
    private Double maxResponseTime;
}