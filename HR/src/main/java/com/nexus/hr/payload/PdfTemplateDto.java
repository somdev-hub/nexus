package com.nexus.hr.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfTemplateDto {

    private Long employeeId;

    private String employeeName;

    private String department;

    private String position;

    private String remarks;

    private Timestamp effectiveFrom;

    private String organizationName;

    private String organizationAddress;

    private String hrContactEmail;

    private String hrContactPhone;
}
