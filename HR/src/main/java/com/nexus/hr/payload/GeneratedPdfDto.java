package com.nexus.hr.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;

/**
 * DTO to store generated PDF files and metadata
 * Used for both HR initialization and promotion responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedPdfDto {

    // File fields
    private MultipartFile joiningLetterPdf;
    private MultipartFile letterOfIntentPdf;
    private String joiningLetterFileName;
    private String letterOfIntentFileName;

    // Promotion fields
    private String promotionLetterUrl;
    private String revisedCompensationCardUrl;

    // Generic document fields
    private Long employeeId;
    private Long hrId;
    private String documentType; // "PROMOTION", "ONBOARDING", etc.

    // Promotion metadata
    private String previousPosition;
    private String newPosition;
    private Timestamp effectiveFrom;
    private Timestamp generatedAt;

    // Compensation details
    private Double basePay;
    private Double hra;
    private Double netPay;
    private Double annualPackage;
}
