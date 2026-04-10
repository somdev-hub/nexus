package com.nexus.hr.payload;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class InitiatePayrollDto {

//    private Long orgId;
//    private Long empId;

    private Map<String, String> org;
    private List<Map<String, String>> employees;
}
