package com.nexus.iam.dto.response;

import tools.jackson.databind.annotation.JsonSerialize;

import java.time.LocalDateTime;
import java.util.List;

public record DeptRoleTable(Long departmentId, String departmentName, String role, Integer noOfEmployees,
                            @JsonSerialize(using = LocalDateTimeSerializer.class)
                            LocalDateTime createdOn, List<String> permissions, String status) {
}
