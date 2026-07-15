package com.nexus.hr.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompanyInsightDto {
    private Long orgId;
    private String orgName;
    private Long totalOpenings;
    private Long totalApplications;
    private Long applicationsThisWeek;
    private Integer avgTimeToFill;
    private List<TopOpeningDto> topOpenings;
    private List<TopRoleDto> topRoles;
    private List<StatusBreakdownDto> statusBreakdown;
    private String hiringTrend; // "up", "down", "stable"
    private Integer trendPercent;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopOpeningDto {
        private Long id;
        private String title;
        private String department;
        private String type;
        private String location;
        private String postedDate;
        private Long applicationsCount;
        private Long viewsCount;
        private String status;
        private Integer daysOpen;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopRoleDto {
        private String role;
        private Integer openings;
        private Long applications;
        private Double conversionRate;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StatusBreakdownDto {
        private String status;
        private Long count;
        private String color;
    }
}
