package com.nexus.hr.payload;

import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
public class HrInitRequestDto {

    private Long employeeId;

    private Long orgId;

    private String department;

    private String title;

    private String remarks;

    private List<HrDocumentDto> hrDocuments;

    private Timestamp effectiveFrom;
}
