package com.nexus.hr.payload.response;

import java.sql.Date;

public record EmployeeDirectoryResponse(
        Long empId,
        String deptName,
        String position,
        Double salary,
        Date joiningDate
) {
}
