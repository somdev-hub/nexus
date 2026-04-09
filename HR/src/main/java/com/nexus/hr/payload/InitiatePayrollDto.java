package com.nexus.hr.payload;

import lombok.Data;

@Data
public class InitiatePayrollDto {

    private Long orgId;
    private Long empId;
}
