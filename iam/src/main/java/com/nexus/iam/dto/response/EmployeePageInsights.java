package com.nexus.iam.dto.response;

import java.util.Map;

public record EmployeePageInsights(Integer totalEmployees, Integer totalDepartments,
                                   Map<String, Integer> employeesPerDepartment, Map<String, Integer> genderRatio,
                                   Integer onNoticePeriod) {
}
