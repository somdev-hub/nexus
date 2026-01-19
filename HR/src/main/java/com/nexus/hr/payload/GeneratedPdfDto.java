package com.nexus.hr.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO to store generated PDF files
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedPdfDto {

    private MultipartFile joiningLetterPdf;

    private MultipartFile letterOfIntentPdf;

    private String joiningLetterFileName;

    private String letterOfIntentFileName;

    private Long employeeId;
}
