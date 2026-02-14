package com.nexus.iam.dto.response;

import tools.jackson.databind.annotation.JsonSerialize;

import java.sql.Date;
import java.time.LocalDateTime;

public record EmployeeDirectoryResponse(
        Long empId,
        String empName,
        String empEmail,
        String deptName,
        String position,
        Double salary,
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        LocalDateTime dateOfJoining
) {
}
