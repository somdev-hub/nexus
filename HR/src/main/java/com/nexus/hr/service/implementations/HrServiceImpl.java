package com.nexus.hr.service.implementations;

import com.nexus.hr.exception.ResourceNotFoundException;
import com.nexus.hr.exception.ServiceLevelException;
import com.nexus.hr.model.entities.*;
import com.nexus.hr.model.enums.HrRequestStatus;
import com.nexus.hr.payload.*;
import com.nexus.hr.repository.HrEntityRepo;
import com.nexus.hr.repository.HrRequestRepo;
import com.nexus.hr.service.interfaces.CommunicationService;
import com.nexus.hr.service.interfaces.HrService;
import com.nexus.hr.utils.CommonConstants;
import com.nexus.hr.utils.CommonUtils;
import com.nexus.hr.utils.RestServices;
import com.nexus.hr.utils.WebConstants;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class HrServiceImpl implements HrService {

    private final HrEntityRepo hrEntityRepo;
    private final ModelMapper modelMapper;
    private final CommunicationService communicationService;
    private final AsyncDocumentService asyncDocumentService;
    private final HrRequestRepo hrRequestsRepo;
    private final WebConstants webConstants;
    private final CommonUtils commonUtils;
    private final RestServices restServices;


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

            // Save HR entity FIRST to get the ID - but don't add compensation yet
            log.info("=== Attempting to save HrEntity for employeeId: {} ===", hrInitRequestDto.getEmployeeId());
            log.debug("HrEntity details: org={}, department={}, positions count={}, documents count={}",
                    hrEntity.getOrg(), hrEntity.getDepartment(),
                    hrEntity.getPositions().size(),
                    hrEntity.getHrDocuments() != null ? hrEntity.getHrDocuments().size() : 0);

            HrEntity savedHrEntity = hrEntityRepo.save(hrEntity);
            log.info("✓ Successfully saved HrEntity with hrId: {}", savedHrEntity.getHrId());

            // Generate PDF template data
            PdfTemplateDto pdfTemplateData = buildPdfTemplateData(hrInitRequestDto, effectiveFrom);

            // Start async PDF generation and DMS uploads in parallel
            log.info("Starting parallel document generation and upload for employee: {}", savedHrEntity.getEmployeeId());
            CompletableFuture<AsyncDocumentService.DocumentResult> joiningLetterFuture =
                    asyncDocumentService.generateAndUploadJoiningLetter(pdfTemplateData, savedHrEntity.getEmployeeId(), savedHrEntity.getHrId());

            CompletableFuture<AsyncDocumentService.DocumentResult> letterOfIntentFuture =
                    asyncDocumentService.generateAndUploadLetterOfIntent(pdfTemplateData, savedHrEntity.getEmployeeId(), savedHrEntity.getHrId());

            CompletableFuture<AsyncDocumentService.DocumentResult> compensationCardFuture =
                    asyncDocumentService.generateAndUploadCompensationCard(pdfTemplateData, savedHrEntity.getEmployeeId(), savedHrEntity.getHrId());

            // Wait for all async operations to complete
            CompletableFuture.allOf(joiningLetterFuture, letterOfIntentFuture, compensationCardFuture).join();
            log.info("All document generation and upload tasks completed for employee: {}", savedHrEntity.getEmployeeId());

            // Get results
            AsyncDocumentService.DocumentResult joiningLetterResult = joiningLetterFuture.join();
            AsyncDocumentService.DocumentResult letterOfIntentResult = letterOfIntentFuture.join();
            AsyncDocumentService.DocumentResult compensationCardResult = compensationCardFuture.join();

            // Validate results
            if (!joiningLetterResult.isSuccess()) {
                ErrorResponseDto error = new ErrorResponseDto();
                error.setMessage("Error uploading Joining Letter to DMS: " + joiningLetterResult.getErrorMessage());
                error.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
                error.setTimestamp(new Timestamp(System.currentTimeMillis()));
                error.setServiceMethod("initHr");
                return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            if (!letterOfIntentResult.isSuccess()) {
                ErrorResponseDto error = new ErrorResponseDto();
                error.setMessage("Error uploading Letter of Intent to DMS: " + letterOfIntentResult.getErrorMessage());
                error.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
                error.setTimestamp(new Timestamp(System.currentTimeMillis()));
                error.setServiceMethod("initHr");
                return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // Extract URLs from results
            String joiningLetterUrl = joiningLetterResult.getDocumentUrl();
            String letterOfIntentUrl = letterOfIntentResult.getDocumentUrl();
            String compensationCardUrl = "";

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

            if (compensationCardResult.isSuccess()) {
                HrDocument hrDocument = compensationCardResult.toHrDocument(compensation);
                // Add document to compensation's compensationCard list
                compensation.getCompensationCard().add(hrDocument);
                compensationCardUrl = compensationCardResult.getDocumentUrl();
            } else {
                ErrorResponseDto error = new ErrorResponseDto();
                error.setMessage("Error uploading Compensation Card to DMS: " + compensationCardResult.getErrorMessage());
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

    @Override
    public ResponseEntity<?> takeActionForHrRequests(Long requestId, HrRequestStatus action, String resolutionRemarks) {
        if (ObjectUtils.isEmpty(requestId)) {
            throw new ServiceLevelException("HR Service", "Request ID cannot be null or empty", "takeActionForHrRequests", "InvalidInput", "Request ID is null or empty");
        }
        try {
            HrRequest hrRequest = hrRequestsRepo.findById(requestId).orElseThrow(() -> new ResourceNotFoundException("HrRequests", "requestId", requestId));
            hrRequest.setStatus(action);
            hrRequest.setResolutionRemarks(resolutionRemarks);
            hrRequest.setResolvedOn(new Timestamp(System.currentTimeMillis()));

            // kafka implementation


            hrRequestsRepo.save(hrRequest);
            return ResponseEntity.ok("HR request with ID " + requestId + " has been " + action.name().toLowerCase() + ".");
        } catch (RuntimeException e) {
            throw new ServiceLevelException("HR Service", "Exception occurred while taking action on HR request", "takeActionForHrRequests", e.getClass().getName(), e.getMessage());
        }
    }

    @Override
    public ResponseEntity<Page<HrRequestDto>> getAllHrRequests(Pageable pageable) {
        try {
            Page<HrRequest> hrRequestsPage = hrRequestsRepo.findAll(pageable);
            Page<HrRequestDto> hrRequestDtoPage = hrRequestsPage.map(request -> {
                HrRequestDto hrRequestDto = modelMapper.map(request, HrRequestDto.class);
                RestPayload restPayload = commonUtils.buildRestPayload(webConstants.getGetUserDetailsUrl(), Map.of("userId", request.getAppliedBy().getEmployeeId().toString()), null, "json");
                ResponseEntity<?> response = restServices.hrRestCall(restPayload.getBuilder().toUriString(), null, restPayload.getHeaders(), HttpMethod.GET, request.getAppliedBy().getHrId());
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> details = (Map<String, String>) response.getBody();
                    hrRequestDto.setEmployeeName(details.get("name"));
                    hrRequestDto.setEmployeeEmail(details.get("email"));
                }
                return hrRequestDto;
            });

            return ResponseEntity.ok(hrRequestDtoPage);
        } catch (RuntimeException e) {
            throw new ServiceLevelException("HR Service", "Exception occurred while fetching HR requests", "getAllHrRequests", e.getClass().getName(), e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> promoteEmployee(Long hrId, Position position, Compensation compensation) {
        return null;
    }

    @Override
    public ResponseEntity<?> rewardAppraisal(Long hrId, Compensation compensation) {
        return null;
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
