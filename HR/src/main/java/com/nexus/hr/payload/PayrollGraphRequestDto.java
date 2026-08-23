package com.nexus.hr.payload;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PayrollGraphRequestDto {
    private List<rolesWithEmpIds> roleEmpIdMap;
    private String month;
    private Integer year;
    private Long orgId;
    @Data
    public static class rolesWithEmpIds {
        private String role;
        private List<Long> empIds;
    }
}
