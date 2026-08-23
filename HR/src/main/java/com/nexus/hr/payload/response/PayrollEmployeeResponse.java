package com.nexus.hr.payload.response;

import com.nexus.hr.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollEmployeeResponse {
    private Long employeeId;
    private String positionTitle;
    private String department;
    private Double monthlySalaryGross;
    private Double monthlySalaryNet;
    private PaymentStatus paymentStatus;
    private String month;
    private Integer year;
    private Timestamp paidOn;
    private String paymentReferenceId;
}

