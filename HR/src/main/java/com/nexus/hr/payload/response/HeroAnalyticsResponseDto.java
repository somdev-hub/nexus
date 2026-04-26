package com.nexus.hr.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class HeroAnalyticsResponseDto {

    private Values totalEmployees;
    private Values presentEmployees;
    private Values onLeaveEmployees;
    private Values openHrRequests;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Values{
        private Integer value;
        private Integer difference;
        private Trend trend;
        private String comparisonWith;
    }

    public enum Trend{
        INCREMENT,
        DECREMENT,
        STABLE
    }
}
