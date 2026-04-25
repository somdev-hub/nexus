package com.nexus.hr.payload.response;

import lombok.Data;

@Data
public class TimeManagementQuickResponseDto {
    private String lastCheckedInTime;
    private String lastCheckedOutTime;
    private String totalBreakTime;
    private String totalWorkHours;
}
