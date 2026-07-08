package com.nexus.hr.payload.response;

import com.nexus.hr.model.enums.HiringStatus;
import com.nexus.hr.model.enums.HiringType;

import java.util.List;

public record RecruitmentFilter(List<HiringType> hiringTypes, List<HiringStatus> hiringStatuses, List<String> orgNames, List<String> locations) {
}
