package com.nexus.hr.service.implementations;

import com.nexus.hr.exception.ServiceLevelException;
import com.nexus.hr.model.entities.*;
import com.nexus.hr.payload.*;
import com.nexus.hr.repository.HrEntityRepo;
import com.nexus.hr.service.interfaces.CommunicationService;
import com.nexus.hr.service.interfaces.HrService;
import com.nexus.hr.utils.CommonConstants;
import com.nexus.hr.utils.CommonUtils;
import com.nexus.hr.utils.RestServices;
import com.nexus.hr.utils.WebConstants;
import com.nexus.hr.views.PdfGeneratorService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class HrServiceImpl implements HrService {

    private final HrEntityRepo hrEntityRepo;

    private final ModelMapper modelMapper;

    private final PdfGeneratorService pdfGeneratorService;

    private final CommonUtils commonUtils;

    private final WebConstants webConstants;

    private final RestServices restServices;

    private final CommunicationService communicationService;


    @Transactional
    @Override
    public ResponseEntity<?> initHr(HrInitRequestDto hrInitRequestDto) {
        ResponseEntity<?> response = null;
        try {
            HrEntity hrEntity = new HrEntity();
            hrEntity.setEmployeeId(hrInitRequestDto.getEmployeeId());
            hrEntity.setOrg(hrInitRequestDto.getOrgId());
            hrEntity.setDepartment(hrInitRequestDto.getDepartment());
            hrEntity.setDateOfJoining(Date.valueOf(LocalDate.now()));
            hrEntity.setIsActive(Boolean.TRUE);

            Position position = new Position();
            position.setTitle(hrInitRequestDto.getTitle());
            position.setRemarks(hrInitRequestDto.getRemarks());
            Timestamp effectiveFrom;
            if (!ObjectUtils.isEmpty(hrInitRequestDto.getEffectiveFrom())) {
                position.setEffectiveFrom(hrInitRequestDto.getEffectiveFrom());
                effectiveFrom = hrInitRequestDto.getEffectiveFrom();
            } else {
                effectiveFrom = new Timestamp(System.currentTimeMillis());
                position.setEffectiveFrom(effectiveFrom);
            }
            position.setIsActive(true);
            position.setHrEntity(hrEntity);
            hrEntity.getPositions().add(position);

            if (!ObjectUtils.isEmpty(hrInitRequestDto.getHrDocuments()) && !hrInitRequestDto.getHrDocuments().isEmpty()) {
                List<HrDocument> hrDocuments = hrInitRequestDto.getHrDocuments().stream().map(document -> {
                    HrDocument hrDocument = modelMapper.map(document, HrDocument.class);
                    hrDocument.setHrEntity(hrEntity);
                    return hrDocument;
                }).toList();
                hrEntity.setHrDocuments(hrDocuments);
            }

            // Generate PDF documents
            PdfTemplateDto pdfTemplateData = buildPdfTemplateData(hrInitRequestDto, effectiveFrom);
            MultipartFile joiningLetterPdf = pdfGeneratorService.generateJoiningLetterPdf(pdfTemplateData);
            MultipartFile letterOfIntentPdf = pdfGeneratorService.generateLetterOfIntentPdf(pdfTemplateData);
            MultipartFile compensationCardPdf = pdfGeneratorService.generateCompensationCardPdf(pdfTemplateData);

            // Save HR entity FIRST to get the ID - but don't add compensation yet
            log.info("=== Attempting to save HrEntity for employeeId: {} ===", hrInitRequestDto.getEmployeeId());
            log.debug("HrEntity details: org={}, department={}, positions count={}, documents count={}",
                hrEntity.getOrg(), hrEntity.getDepartment(),
                hrEntity.getPositions().size(),
                hrEntity.getHrDocuments() != null ? hrEntity.getHrDocuments().size() : 0);

            HrEntity savedHrEntity = hrEntityRepo.save(hrEntity);
            log.info("✓ Successfully saved HrEntity with hrId: {}", savedHrEntity.getHrId());

            // Upload documents to DMS (use savedHrEntity.getHrId() for logging)
            ResponseEntity<?> joiningLetterDmsResponse = callDmsToUpload(joiningLetterPdf, hrInitRequestDto.getEmployeeId(), "Joining_Letter_" + savedHrEntity.getEmployeeId() + ".pdf", "JOINING_LETTER", savedHrEntity.getHrId());

            ResponseEntity<?> letterOfIntentDmsResponse = callDmsToUpload(letterOfIntentPdf, hrInitRequestDto.getEmployeeId(), "Letter_Of_Intent_" + savedHrEntity.getEmployeeId() + ".pdf", "LETTER_OF_INTENT", savedHrEntity.getHrId());

            ResponseEntity<?> compensationCardDmsResponse = callDmsToUpload(compensationCardPdf, hrInitRequestDto.getEmployeeId(), "Compensation_Card_" + savedHrEntity.getEmployeeId() + ".pdf", "COMPENSATION_CARD", savedHrEntity.getHrId());

            String joiningLetterUrl = "";
            String letterOfIntentUrl = "";
            String compensationCardUrl = "";

            if (joiningLetterDmsResponse.getStatusCode().is2xxSuccessful()) {
                @SuppressWarnings("unchecked") Map<String, String> responseBody = (Map<String, String>) joiningLetterDmsResponse.getBody();
                assert responseBody != null;
                if (responseBody.containsKey("documentUrl")) {
                    HrDocument hrDocument = new HrDocument();
                    hrDocument.setDocumentUrl(responseBody.get("documentUrl"));
                    hrDocument.setDocumentName(responseBody.get("documentName"));
                    hrDocument.setHrDocumentType(responseBody.get("documentType"));
                    hrDocument.setPosition(position);
                    joiningLetterUrl = responseBody.get("documentUrl");
                }
            } else {
                ErrorResponseDto error = new ErrorResponseDto();
                error.setMessage("Error uploading Joining Letter to DMS");
                error.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
                error.setTimestamp(new Timestamp(System.currentTimeMillis()));
                error.setServiceMethod("initHr");
                return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            if (letterOfIntentDmsResponse.getStatusCode().is2xxSuccessful()) {
                @SuppressWarnings("unchecked") Map<String, String> responseBody = (Map<String, String>) letterOfIntentDmsResponse.getBody();
                assert responseBody != null;
                if (responseBody.containsKey("documentUrl")) {
                    HrDocument hrDocument = new HrDocument();
                    hrDocument.setDocumentUrl(responseBody.get("documentUrl"));
                    hrDocument.setDocumentName(responseBody.get("documentName"));
                    hrDocument.setHrDocumentType(responseBody.get("documentType"));
                    hrDocument.setPosition(position);
                    letterOfIntentUrl = responseBody.get("documentUrl");
                }
            } else {
                ErrorResponseDto error = new ErrorResponseDto();
                error.setMessage("Error uploading Letter of Intent to DMS");
                error.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
                error.setTimestamp(new Timestamp(System.currentTimeMillis()));
                error.setServiceMethod("initHr");
                return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // Build compensation with bonuses, deductions, and bank records
            log.info("=== Building Compensation with bonuses: {}, deductions: {}, bank records: {} ===",
                hrInitRequestDto.getCompensation().getBonuses().size(),
                hrInitRequestDto.getCompensation().getDeductions().size(),
                hrInitRequestDto.getCompensation().getBankRecords().size());

            Compensation compensation = modelMapper.map(hrInitRequestDto.getCompensation(), Compensation.class);
            compensation.setBonuses(hrInitRequestDto.getCompensation().getBonuses().stream()
                    .map(bonus -> {
                        Bonus bonusEntity = modelMapper.map(bonus, Bonus.class);
                        bonusEntity.setCompensation(compensation);
                        return bonusEntity;
                    }).toList());

            compensation.setDeductions(hrInitRequestDto.getCompensation().getDeductions().stream()
                    .map(deductionDto -> {
                        Deduction deductionEntity = modelMapper.map(deductionDto, Deduction.class);
                        deductionEntity.setCompensation(compensation);
                        return deductionEntity;
                    }).toList());

            log.debug("=== Processing Bank Records for encryption ===");
            compensation.setBankRecords(hrInitRequestDto.getCompensation().getBankRecords().stream()
                    .map(bankRecordDto -> {
                        BankRecord bankRecord = modelMapper.map(bankRecordDto, BankRecord.class);

                        // Log bank record details before encryption (sensitive data masked)
                        log.debug("BankRecord: bank={}, accountType={}, hasAccountNumber={}, hasIfsc={}",
                            bankRecord.getBankName() != null ? "***" : "null",
                            bankRecord.getAccountType(),
                            bankRecord.getAccountNumber() != null,
                            bankRecord.getIfscCode() != null);

                        bankRecord.setCompensation(compensation);
                        return bankRecord;
                    }).toList());
            log.info("✓ Compensation entity built successfully");

            if (compensationCardDmsResponse.getStatusCode().is2xxSuccessful()) {
                @SuppressWarnings("unchecked") Map<String, String> responseBody = (Map<String, String>) compensationCardDmsResponse.getBody();
                assert responseBody != null;
                if (responseBody.containsKey("documentUrl")) {
                    HrDocument hrDocument = new HrDocument();
                    hrDocument.setDocumentUrl(responseBody.get("documentUrl"));
                    hrDocument.setDocumentName(responseBody.get("documentName"));
                    hrDocument.setHrDocumentType(responseBody.get("documentType"));
                    hrDocument.setCompensation(compensation);

                    // Add document to compensation's compensationCard list
                    compensation.getCompensationCard().add(hrDocument);

                    compensationCardUrl = responseBody.get("documentUrl");
                }
            } else {
                ErrorResponseDto error = new ErrorResponseDto();
                error.setMessage("Error uploading Compensation Card to DMS");
                error.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
                error.setTimestamp(new Timestamp(System.currentTimeMillis()));
                error.setServiceMethod("initHr");
                return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // Link compensation to hrEntity
            compensation.setHrEntity(savedHrEntity);
            savedHrEntity.setCompensation(compensation);

            // Send communication email (this runs in its own transaction for logging)
            EmailCommunicationDto emailCommunicationDto = new EmailCommunicationDto();
            emailCommunicationDto.setSenderEmail("hr@nexus.com");
            emailCommunicationDto.setRecipientEmails(List.of(hrInitRequestDto.getPersonalEmail()));
            emailCommunicationDto.setSubject("Update on your application");

            // Use email template with placeholders instead of String.formatted()
            emailCommunicationDto.setBody(CommonConstants.HR_INIT_EMAIL_TEMPLATE);

            // Create placeholders map for dynamic content replacement
            Map<String, Object> placeholders = new HashMap<>();
            placeholders.put("name", hrInitRequestDto.getFullName());
            placeholders.put("employeeId", savedHrEntity.getEmployeeId());
            placeholders.put("department", hrInitRequestDto.getDepartment());
            placeholders.put("position", hrInitRequestDto.getTitle());
            placeholders.put("dateOfJoining", savedHrEntity.getDateOfJoining().toString());
            placeholders.put("organizationName", "Nexus Corporation");
            emailCommunicationDto.setPlaceholders(placeholders);

            // Set attachments with proper MIME types
            emailCommunicationDto.setAttachments(List.of(
                    new EmailAttachmentDto("Joining_Letter_" + savedHrEntity.getEmployeeId() + ".pdf", "application/pdf", joiningLetterUrl),
                    new EmailAttachmentDto("Letter_Of_Intent_" + savedHrEntity.getEmployeeId() + ".pdf", "application/pdf", letterOfIntentUrl),
                    new EmailAttachmentDto("Compensation_Card_" + savedHrEntity.getEmployeeId() + ".pdf", "application/pdf", compensationCardUrl)
            ));

            // Send email - if this fails, it won't affect the transaction
            try {
                communicationService.sendCommunicationOverEmail(emailCommunicationDto);
                log.info("Welcome email sent successfully to {} for employee ID: {}",
                    hrInitRequestDto.getPersonalEmail(), savedHrEntity.getEmployeeId());
            } catch (Exception emailException) {
                // Log the email error but don't throw exception - prevents transaction rollback
                log.error("Email sending failed but HR entity was created successfully. Employee ID: {}, Email: {}, Error: {}",
                    savedHrEntity.getEmployeeId(), hrInitRequestDto.getPersonalEmail(),
                    emailException.getMessage(), emailException);
                // Continue with the transaction - email failure shouldn't prevent HR creation
            }

            // Final save with all relationships (cascade will save everything)
            log.info("=== Attempting final save of HrEntity with all relationships ===");
            log.debug("Saving compensation with {} bonuses, {} deductions, {} bank records",
                savedHrEntity.getCompensation().getBonuses().size(),
                savedHrEntity.getCompensation().getDeductions().size(),
                savedHrEntity.getCompensation().getBankRecords().size());

            try {
                hrEntityRepo.save(savedHrEntity);
                log.info("✓✓✓ Successfully saved HrEntity with all relationships. HrId: {}", savedHrEntity.getHrId());
            } catch (Exception saveException) {
                log.error("✗✗✗ FAILED to save HrEntity with relationships. Error: {}", saveException.getMessage());
                log.error("Exception details:", saveException);

                // Check for specific database constraint violations
                String errorMsg = saveException.getMessage().toLowerCase();
                if (errorMsg.contains("null value") && errorMsg.contains("violates not-null")) {
                    log.error("NULL CONSTRAINT VIOLATION - Check which field is null in the logs above");
                } else if (errorMsg.contains("foreign key") || errorMsg.contains("violates foreign key")) {
                    log.error("FOREIGN KEY VIOLATION - Check relationship mappings");
                } else if (errorMsg.contains("unique constraint")) {
                    log.error("UNIQUE CONSTRAINT VIOLATION - Duplicate value detected");
                } else if (errorMsg.contains("invalid input syntax for type")) {
                    log.error("DATA TYPE MISMATCH - Check encryption converter or data types");
                }

                throw saveException;
            }

            response = ResponseEntity.ok(HrInitResponse.builder()
                    .hrId(savedHrEntity.getHrId())
                    .joiningLetterUrl(joiningLetterUrl)
                    .letterOfIntentUrl(letterOfIntentUrl)
                    .compensationCardUrl(compensationCardUrl)
                    .build());

        } catch (Exception e) {
            throw new ServiceLevelException("HR Service", "Exception occurred while initializing HR module", "initHr", e.getClass().getName(), e.getMessage());
        }

        return response;
    }

    /**
     * Upload document to DMS service
     * This method makes HTTP calls and doesn't need transaction management
     */
    private ResponseEntity<?> callDmsToUpload(MultipartFile file, Long userId, String fileName, String fileType, Long hrId) {
        ResponseEntity<?> response;
        try {
            if (!ObjectUtils.isEmpty(file)) {
                Map<String, Object> payload = new ConcurrentHashMap<>();
                Map<String, Object> dto = new HashMap<>();
                dto.put("userId", userId);
                dto.put("fileName", fileName);
                dto.put("remarks", "HR Document Upload");
                dto.put("documentType", fileType);

                payload.put("file", file);
                payload.put("dto", dto);

                Map<String, String> headers = new HashMap<>();
                headers.put(CommonConstants.CONTENT_TYPE, CommonConstants.MULTIPART_FORM_DATA);
                headers.put(CommonConstants.AUTHORIZATION, commonUtils.getToken());

                UriComponentsBuilder url = UriComponentsBuilder.fromUriString(webConstants.getDmsOrgDocumentUploadUrl());

                response = restServices.hrRestCall(url.toUriString(), payload, headers, HttpMethod.POST, hrId);

            } else {
                response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            // Don't throw ServiceLevelException here - return error response instead
            // This prevents marking the parent transaction as rollback-only
            ErrorResponseDto error = new ErrorResponseDto();
            error.setMessage("Failed to upload document to DMS: " + e.getMessage());
            error.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            error.setTimestamp(new Timestamp(System.currentTimeMillis()));
            error.setServiceMethod("callDmsToUpload");
            response = new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return response;
    }

    /**
     * Build PDF template data from HR initialization request
     */
    private PdfTemplateDto buildPdfTemplateData(HrInitRequestDto hrInitRequestDto, Timestamp effectiveFrom) {
        PdfTemplateDto.PdfTemplateDtoBuilder builder = PdfTemplateDto.builder()
                .employeeId(hrInitRequestDto.getEmployeeId())
                .employeeName("Employee") // You may need to fetch this from employee service
                .department(hrInitRequestDto.getDepartment())
                .position(hrInitRequestDto.getTitle())
                .remarks(hrInitRequestDto.getRemarks())
                .effectiveFrom(effectiveFrom)
                .organizationName("Organization") // Configure from properties
                .organizationAddress("Organization Address") // Configure from properties
                .hrContactEmail("hr@organization.com") // Configure from properties
                .hrContactPhone("+1-XXX-XXX-XXXX"); // Configure from properties

        // Add compensation details if available
        if (hrInitRequestDto.getCompensation() != null) {
            builder.basePay(hrInitRequestDto.getCompensation().getBasePay())
                    .hra(hrInitRequestDto.getCompensation().getHra())
                    .bonuses(hrInitRequestDto.getCompensation().getBonuses().stream()
                            .map(bonus -> modelMapper.map(bonus, Bonus.class))
                            .toList())
                    .deductions(hrInitRequestDto.getCompensation().getDeductions().stream()
                            .map(deduction -> modelMapper.map(deduction, Deduction.class))
                            .toList())
                    .netPay(hrInitRequestDto.getCompensation().getNetPay())
                    .gratuity(hrInitRequestDto.getCompensation().getGratuity())
                    .pf(hrInitRequestDto.getCompensation().getPf())
                    .annualPackage(hrInitRequestDto.getCompensation().getAnnualPackage())
                    .total(hrInitRequestDto.getCompensation().getTotal())
                    .netMonthlyPay(hrInitRequestDto.getCompensation().getNetMonthlyPay());
        }

        return builder.build();
    }
}
