package com.nexus.hr.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStatsDto {
    private String label;
    private String value;
    private String change;
    private String changeType; // "positive", "negative", "neutral"
    private String icon;
    private String color;
}