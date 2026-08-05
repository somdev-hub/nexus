package com.nexus.nexusbuddy.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HourlyHitsDataPoint {
    private String hour; // ISO string or formatted hour
    private Long totalHits;
    private Long successHits;
    private Long failureHits;
}