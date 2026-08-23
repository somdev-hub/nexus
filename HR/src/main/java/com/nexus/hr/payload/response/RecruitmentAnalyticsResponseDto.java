package com.nexus.hr.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class RecruitmentAnalyticsResponseDto {
    private Values openRoles;
    private Values currentApplications;
    private Values underReview;
    private Values offerSent;
    private Values recruitmentTAT;
    private Values offerAcceptance;

    public enum Type {
        DIFFERENCE_COMPARISON,
        VALUE_COMPARISON
    }

    public enum Trend {
        INCREMENT,
        DECREMENT,
        STABLE
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Values {
        private Integer value;
        private Type type;
        private Integer difference;
        private Trend trend;
        private String description;
        private String comparisonWith;
    }
}
