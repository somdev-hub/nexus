package com.nexus.iam.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.LocalDateTime;
import java.util.List;

public record DeptRoleTable(Long departmentId, String departmentName, String role, Integer noOfEmployees,
        @JsonSerialize(using = LocalDateTimeSerializer.class) LocalDateTime createdOn, List<String> permissions,
        String status) {
}
