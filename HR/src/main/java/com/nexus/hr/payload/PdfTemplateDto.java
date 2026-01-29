package com.nexus.hr.payload;

import com.nexus.hr.model.entities.Bonus;
import com.nexus.hr.model.entities.Deduction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfTemplateDto {

    private Long employeeId;

    private String employeeName;

    private String department;

    private String position;

    private String title;

    private String previousTitle;

    private String remarks;

    private Timestamp effectiveFrom;

    private String organizationName;

    private String organizationAddress;

    private String hrContactEmail;

    private String hrContactPhone;

    // Compensation details
    private Double basePay;

    private Double hra;

    private List<Bonus> bonuses;

    private List<Deduction> deductions;

    private Double netPay;

    private Double gratuity;

    private Double pf;

    private String annualPackage;

    private Double total;

    private Double netMonthlyPay;
}
