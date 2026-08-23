package com.nexus.iam.dto;

import lombok.Data;

import java.util.List;

@Data
public class InitiateEmployeePayrollDto {

    private Long orgId;
    private List<Long> employeeIds;
}
