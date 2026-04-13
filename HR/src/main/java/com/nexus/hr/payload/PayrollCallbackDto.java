package com.nexus.hr.payload;

import lombok.Data;

@Data
public class PayrollCallbackDto {
    private Long payrollId;
    private String paymentReferenceId;
    private Boolean success;
}
