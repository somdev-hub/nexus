package com.nexus.hr.service.implementations;

import com.nexus.hr.exception.ResourceNotFoundException;
import com.nexus.hr.exception.ServiceLevelException;
import com.nexus.hr.model.entities.*;
import com.nexus.hr.model.enums.HrRequestStatus;
import com.nexus.hr.payload.*;
import com.nexus.hr.payload.response.EmployeeDirectoryResponse;
import com.nexus.hr.repository.HrEntityRepo;
import com.nexus.hr.repository.HrRequestRepo;
import com.nexus.hr.service.interfaces.CommunicationService;
import com.nexus.hr.service.interfaces.HrService;
import com.nexus.hr.utils.CommonUtils;
import com.nexus.hr.utils.LeaveAllocationUtils;
import com.nexus.hr.utils.RestServices;
import com.nexus.hr.utils.WebConstants;
import com.nexus.hr.views.CommunicationTemplateBuilder;
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
    private final CommunicationTemplateBuilder communicationTemplateBuilder;
    private final LeaveAllocationUtils leaveAllocationUtils;

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

            if (!ObjectUtils.isEmpty(hrInitRequestDto.getHrDocuments())
                    && !hrInitRequestDto.getHrDocuments().isEmpty()) {
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
            log.info("Starting parallel document generation and upload for employee: {}",
                    savedHrEntity.getEmployeeId());
            CompletableFuture<AsyncDocumentService.DocumentResult> joiningLetterFuture = asyncDocumentService
                    .generateAndUploadJoiningLetter(pdfTemplateData, savedHrEntity.getEmployeeId(),
                            savedHrEntity.getHrId());

            CompletableFuture<AsyncDocumentService.DocumentResult> letterOfIntentFuture = asyncDocumentService
                    .generateAndUploadLetterOfIntent(pdfTemplateData, savedHrEntity.getEmployeeId(),
                            savedHrEntity.getHrId());

            CompletableFuture<AsyncDocumentService.DocumentResult> compensationCardFuture = asyncDocumentService
                    .generateAndUploadCompensationCard(pdfTemplateData, savedHrEntity.getEmployeeId(),
                            savedHrEntity.getHrId());

            // Wait for all async operations to complete
            CompletableFuture.allOf(joiningLetterFuture, letterOfIntentFuture, compensationCardFuture).join();
            log.info("All document generation and upload tasks completed for employee: {}",
                    savedHrEntity.getEmployeeId());

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
                error.setMessage(
                        "Error uploading Compensation Card to DMS: " + compensationCardResult.getErrorMessage());
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
            emailCommunicationDto.setBody(communicationTemplateBuilder.buildHrInitEmailTemplate());

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
                    new EmailAttachmentDto("Joining_Letter_" + savedHrEntity.getEmployeeId() + ".pdf",
                            "application/pdf", joiningLetterUrl),
                    new EmailAttachmentDto("Letter_Of_Intent_" + savedHrEntity.getEmployeeId() + ".pdf",
                            "application/pdf", letterOfIntentUrl),
                    new EmailAttachmentDto("Compensation_Card_" + savedHrEntity.getEmployeeId() + ".pdf",
                            "application/pdf", compensationCardUrl)));

            // Send email - if this fails, it won't affect the transaction
            try {
                communicationService.sendCommunicationOverEmail(emailCommunicationDto);
                log.info("Welcome email sent successfully to {} for employee ID: {}",
                        hrInitRequestDto.getPersonalEmail(), savedHrEntity.getEmployeeId());
            } catch (Exception emailException) {
                // Log the email error but don't throw exception - prevents transaction rollback
                log.error(
                        "Email sending failed but HR entity was created successfully. Employee ID: {}, Email: {}, Error: {}",
                        savedHrEntity.getEmployeeId(), hrInitRequestDto.getPersonalEmail(),
                        emailException.getMessage(), emailException);
                // Continue with the transaction - email failure shouldn't prevent HR creation
            }

            // Initialize leave allocations for the new employee
            log.info("=== Initializing leave allocations for employee: {} ===", savedHrEntity.getEmployeeId());
            leaveAllocationUtils.initializeLeaveAllocations(savedHrEntity);
            log.info("✓ Leave allocations initialized successfully");

            // Final save with all relationships (cascade will save everything)
            log.info("=== Attempting final save of HrEntity with all relationships ===");
            log.debug("Saving compensation with {} bonuses, {} deductions, {} bank records, {} leave allocations",
                    savedHrEntity.getCompensation().getBonuses().size(),
                    savedHrEntity.getCompensation().getDeductions().size(),
                    savedHrEntity.getCompensation().getBankRecords().size(),
                    savedHrEntity.getLeaveAllocations().size());

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
            throw new ServiceLevelException("HR Service", "Exception occurred while initializing HR module", "initHr",
                    e.getClass().getName(), e.getMessage());
        }

        return response;
    }

    @Override
    public ResponseEntity<?> takeActionForHrRequests(Long requestId, HrRequestStatus action, String resolutionRemarks) {
        if (ObjectUtils.isEmpty(requestId)) {
            throw new ServiceLevelException("HR Service", "Request ID cannot be null or empty",
                    "takeActionForHrRequests", "InvalidInput", "Request ID is null or empty");
        }
        try {
            HrRequest hrRequest = hrRequestsRepo.findById(requestId)
                    .orElseThrow(() -> new ResourceNotFoundException("HrRequests", "requestId", requestId));
            hrRequest.setStatus(action);
            hrRequest.setResolutionRemarks(resolutionRemarks);
            hrRequest.setResolvedOn(new Timestamp(System.currentTimeMillis()));

            // kafka implementation

            hrRequestsRepo.save(hrRequest);
            return ResponseEntity
                    .ok("HR request with ID " + requestId + " has been " + action.name().toLowerCase() + ".");
        } catch (RuntimeException e) {
            throw new ServiceLevelException("HR Service", "Exception occurred while taking action on HR request",
                    "takeActionForHrRequests", e.getClass().getName(), e.getMessage());
        }
    }

    @Override
    public ResponseEntity<Page<HrRequestDto>> getAllHrRequests(Pageable pageable) {
        try {
            Page<HrRequest> hrRequestsPage = hrRequestsRepo.findAll(pageable);
            Page<HrRequestDto> hrRequestDtoPage = hrRequestsPage.map(request -> {
                HrRequestDto hrRequestDto = modelMapper.map(request, HrRequestDto.class);
                RestPayload restPayload = commonUtils.buildRestPayload(webConstants.getGetUserDetailsUrl(),
                        Map.of("userId", request.getAppliedBy().getEmployeeId().toString()), null, "json");
                ResponseEntity<?> response = restServices.hrRestCall(restPayload.getBuilder().toUriString(), null,
                        restPayload.getHeaders(), HttpMethod.GET, request.getAppliedBy().getHrId());
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
            throw new ServiceLevelException("HR Service", "Exception occurred while fetching HR requests",
                    "getAllHrRequests", e.getClass().getName(), e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> promoteEmployee(Long hrId, Position position, CompensationDto compensation) {
        if (ObjectUtils.isEmpty(hrId) || ObjectUtils.isEmpty(position) || ObjectUtils.isEmpty(compensation)) {
            throw new ServiceLevelException("HR Service", "HR ID, Position, and Compensation cannot be null or empty",
                    "promoteEmployee", "InvalidInput", "One or more inputs are null or empty");
        }
        try {
            log.info("=== Starting promotion process for hrId: {} ===", hrId);

            HrEntity hrEntity = hrEntityRepo.findById(hrId)
                    .orElseThrow(() -> new ResourceNotFoundException("HrEntity", "hrId", hrId));

            // Deactivate last position and set end date
            log.info("Deactivating previous position for employee: {}", hrEntity.getEmployeeId());
            Position lastPosition = hrEntity.getPositions().getLast();
            lastPosition.setIsActive(Boolean.FALSE);
            lastPosition.setLastEffectiveDate(new Timestamp(System.currentTimeMillis()));

            // Activate new position
            position.setIsActive(Boolean.TRUE);
            position.setHrEntity(hrEntity);
            position.setEffectiveFrom(new Timestamp(System.currentTimeMillis()));
            hrEntity.getPositions().add(position);
            log.info("✓ New position {} added for employee: {}", position.getTitle(), hrEntity.getEmployeeId());

            // Update compensation
            log.info("=== Updating compensation details for promotion ===");
            Compensation lastCompensation = hrEntity.getCompensation();
            if (!ObjectUtils.isEmpty(compensation.getBasePay()))
                lastCompensation.setBasePay(compensation.getBasePay());
            if (!ObjectUtils.isEmpty(compensation.getHra()))
                lastCompensation.setHra(compensation.getHra());
            if (!ObjectUtils.isEmpty(compensation.getNetPay()))
                lastCompensation.setNetPay(compensation.getNetPay());
            if (!ObjectUtils.isEmpty(compensation.getGratuity()))
                lastCompensation.setGratuity(compensation.getGratuity());
            if (!ObjectUtils.isEmpty(compensation.getPf()))
                lastCompensation.setPf(compensation.getPf());
            if (!ObjectUtils.isEmpty(compensation.getAnnualPackage()))
                lastCompensation.setAnnualPackage(compensation.getAnnualPackage());
            if (!ObjectUtils.isEmpty(compensation.getInsurancePremium()))
                lastCompensation.setInsurancePremium(compensation.getInsurancePremium());
            if (!ObjectUtils.isEmpty(compensation.getGrossPay()))
                lastCompensation.setGrossPay(compensation.getGrossPay());
            if (!ObjectUtils.isEmpty(compensation.getBonuses())) {
                log.debug("Updating {} bonuses", compensation.getBonuses().size());
                lastCompensation.setBonuses(compensation.getBonuses().stream()
                        .map(bonus -> {
                            Bonus bonusEntity = modelMapper.map(bonus, Bonus.class);
                            bonusEntity.setCompensation(lastCompensation);
                            return bonusEntity;
                        }).toList());
            }
            if (!ObjectUtils.isEmpty(compensation.getDeductions())) {
                log.debug("Updating {} deductions", compensation.getDeductions().size());
                lastCompensation.setDeductions(compensation.getDeductions().stream()
                        .map(deductionDto -> {
                            Deduction deductionEntity = modelMapper.map(deductionDto, Deduction.class);
                            deductionEntity.setCompensation(lastCompensation);
                            return deductionEntity;
                        }).toList());
            }
            hrEntity.setCompensation(lastCompensation);
            log.info("✓ Compensation updated successfully");

            // Build PDF template data for promotion documents
            log.info("Building PDF template data for promotion documents");
            PdfTemplateDto pdfTemplateData = buildPdfTemplateData(HrInitRequestDto.builder()
                    .employeeId(hrEntity.getEmployeeId())
                    .department(hrEntity.getDepartment())
                    .title(position.getTitle())
                    .remarks(position.getRemarks())
                    .compensation(compensation).build(), position.getEffectiveFrom());

            // Generate promotion letter and revised compensation card asynchronously in
            // parallel
            log.info("Starting parallel document generation: promotion letter and revised compensation card");
            CompletableFuture<AsyncDocumentService.DocumentResult> promotionLetterFuture = asyncDocumentService
                    .generateAndUploadPromotionLetter(pdfTemplateData, hrEntity.getEmployeeId(), hrEntity.getHrId());

            CompletableFuture<AsyncDocumentService.DocumentResult> revisedCompensationCardFuture = asyncDocumentService
                    .generateAndUploadCompensationCard(pdfTemplateData, hrEntity.getEmployeeId(), hrEntity.getHrId());

            // Wait for both async operations to complete
            CompletableFuture.allOf(promotionLetterFuture, revisedCompensationCardFuture).join();
            log.info("Document generation and upload tasks completed for employee: {}", hrEntity.getEmployeeId());

            // Get results
            AsyncDocumentService.DocumentResult promotionLetterResult = promotionLetterFuture.join();
            AsyncDocumentService.DocumentResult revisedCompensationCardResult = revisedCompensationCardFuture.join();

            // Validate promotion letter
            if (!promotionLetterResult.isSuccess()) {
                ErrorResponseDto error = new ErrorResponseDto();
                error.setMessage("Error uploading Promotion Letter to DMS: " + promotionLetterResult.getErrorMessage());
                error.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
                error.setTimestamp(new Timestamp(System.currentTimeMillis()));
                error.setServiceMethod("promoteEmployee");
                return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            log.info("✓ Promotion letter generated and uploaded successfully");

            // Validate revised compensation card
            if (!revisedCompensationCardResult.isSuccess()) {
                ErrorResponseDto error = new ErrorResponseDto();
                error.setMessage("Error uploading Revised Compensation Card to DMS: "
                        + revisedCompensationCardResult.getErrorMessage());
                error.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
                error.setTimestamp(new Timestamp(System.currentTimeMillis()));
                error.setServiceMethod("promoteEmployee");
                return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            log.info("✓ Revised compensation card generated and uploaded successfully");

            // Add documents to HR entity
            String promotionLetterUrl = promotionLetterResult.getDocumentUrl();
            String revisedCompensationCardUrl = revisedCompensationCardResult.getDocumentUrl();

            // Add revised compensation card to compensation's card list
            HrDocument revisedCardDocument = revisedCompensationCardResult.toHrDocument(lastCompensation);
            lastCompensation.getCompensationCard().add(revisedCardDocument);

            // Save updated HR entity with all relationships
            log.info("=== Saving updated HrEntity with promotion and new documents ===");
            try {
                hrEntityRepo.save(hrEntity);
                log.info("✓✓✓ Successfully saved HrEntity after promotion. HrId: {}", hrEntity.getHrId());
            } catch (Exception saveException) {
                log.error("✗✗✗ FAILED to save HrEntity after promotion. Error: {}", saveException.getMessage());
                log.error("Exception details:", saveException);
                throw saveException;
            }

            // Send promotion email notification
            log.info("=== Sending promotion notification email to employee {} ===", hrEntity.getEmployeeId());
            try {
                // Fetch employee details from user service to get email
                RestPayload restPayload = commonUtils.buildRestPayload(webConstants.getGetUserDetailsUrl(),
                        Map.of("userId", hrEntity.getEmployeeId().toString()), null, "json");
                ResponseEntity<?> userResponse = restServices.hrRestCall(restPayload.getBuilder().toUriString(), null,
                        restPayload.getHeaders(), HttpMethod.GET, hrEntity.getHrId());

                String employeeEmail = null;
                String employeeName = "Employee";

                if (userResponse.getStatusCode().is2xxSuccessful() && userResponse.getBody() != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> userDetails = (Map<String, String>) userResponse.getBody();
                    employeeEmail = userDetails.get("email");
                    String firstName = userDetails.getOrDefault("firstName", "");
                    String lastName = userDetails.getOrDefault("lastName", "");
                    employeeName = (firstName + " " + lastName).trim();
                }

                if (employeeEmail != null) {
                    EmailCommunicationDto emailCommunicationDto = new EmailCommunicationDto();
                    emailCommunicationDto.setSenderEmail("hr@nexus.com");
                    emailCommunicationDto.setRecipientEmails(List.of(employeeEmail));
                    emailCommunicationDto.setSubject("Congratulations on Your Promotion!");
                    emailCommunicationDto.setBody(communicationTemplateBuilder.buildPromotionEmailTemplate());

                    // Create placeholders map for dynamic content replacement
                    Map<String, Object> placeholders = new HashMap<>();
                    placeholders.put("employeeName", employeeName);
                    placeholders.put("organizationName", "Nexus Corporation");
                    placeholders.put("previousPosition", lastPosition.getTitle());
                    placeholders.put("newPosition", position.getTitle());
                    placeholders.put("department", hrEntity.getDepartment());
                    placeholders.put("effectiveDate",
                            new java.text.SimpleDateFormat("dd-MM-yyyy").format(position.getEffectiveFrom()));
                    placeholders.put("basePay", String.format("₹%.2f", lastCompensation.getBasePay()));
                    placeholders.put("hra", String.format("₹%.2f", lastCompensation.getHra()));
                    placeholders.put("grossPay", String.format("₹%.2f", lastCompensation.getGrossPay()));
                    placeholders.put("annualPackage",
                            lastCompensation.getAnnualPackage() != null ? lastCompensation.getAnnualPackage()
                                    : String.format("₹%.2f", lastCompensation.getInsurancePremium()));
                    placeholders.put("hrEmail", "hr@nexus.com");
                    emailCommunicationDto.setPlaceholders(placeholders);

                    // Set attachments with promotion documents
                    emailCommunicationDto.setAttachments(List.of(
                            new EmailAttachmentDto("Promotion_Letter_" + hrEntity.getEmployeeId() + ".pdf",
                                    "application/pdf", promotionLetterUrl),
                            new EmailAttachmentDto("Revised_Compensation_Card_" + hrEntity.getEmployeeId() + ".pdf",
                                    "application/pdf", revisedCompensationCardUrl)));

                    communicationService.sendCommunicationOverEmail(emailCommunicationDto);
                    log.info("✓ Promotion notification email sent successfully to employee: {}",
                            hrEntity.getEmployeeId());
                } else {
                    log.warn("Could not fetch employee email from user service for employee ID: {}",
                            hrEntity.getEmployeeId());
                }
            } catch (Exception emailException) {
                // Log the email error but don't throw exception - prevents transaction rollback
                log.error("Email sending failed but promotion was processed successfully. Employee ID: {}, Error: {}",
                        hrEntity.getEmployeeId(), emailException.getMessage(), emailException);
            }

            // Build response with promotion details and document URLs
            GeneratedPdfDto promotionResponse = GeneratedPdfDto.builder()
                    .hrId(hrEntity.getHrId())
                    .employeeId(hrEntity.getEmployeeId())
                    .documentType("PROMOTION")
                    .promotionLetterUrl(promotionLetterUrl)
                    .revisedCompensationCardUrl(revisedCompensationCardUrl)
                    .previousPosition(lastPosition.getTitle())
                    .newPosition(position.getTitle())
                    .effectiveFrom(position.getEffectiveFrom())
                    .basePay(lastCompensation.getBasePay())
                    .hra(lastCompensation.getHra())
                    .netPay(lastCompensation.getNetPay())
                    .annualPackage(lastCompensation.getAnnualPackage() != null
                            ? lastCompensation.getAnnualPackage()
                            : String.valueOf(lastCompensation.getGrossPay() * 12))
                    .generatedAt(new Timestamp(System.currentTimeMillis()))
                    .build();

            log.info("✓ Promotion completed successfully for employee {} with new position: {}",
                    hrEntity.getEmployeeId(), position.getTitle());

            return ResponseEntity.ok(promotionResponse);

        } catch (ResourceNotFoundException | ServiceLevelException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceLevelException("HR Service", "Exception occurred while promoting employee",
                    "promoteEmployee", e.getClass().getName(), e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> rewardAppraisal(Long hrId, CompensationDto compensation) {
        if (ObjectUtils.isEmpty(hrId) || ObjectUtils.isEmpty(compensation)) {
            throw new ServiceLevelException("HR Service", "HR ID and Compensation cannot be null or empty",
                    "rewardAppraisal", "InvalidInput", "One or more inputs are null or empty");
        }
        try {
            log.info("=== Starting reward appraisal process for hrId: {} ===", hrId);

            HrEntity hrEntity = hrEntityRepo.findById(hrId)
                    .orElseThrow(() -> new ResourceNotFoundException("HrEntity", "hrId", hrId));

            // Update compensation details
            log.info("=== Updating compensation details for reward appraisal ===");
            Compensation currentCompensation = hrEntity.getCompensation();
            if (!ObjectUtils.isEmpty(compensation.getBasePay()))
                currentCompensation.setBasePay(compensation.getBasePay());
            if (!ObjectUtils.isEmpty(compensation.getHra()))
                currentCompensation.setHra(compensation.getHra());
            if (!ObjectUtils.isEmpty(compensation.getNetPay()))
                currentCompensation.setNetPay(compensation.getNetPay());
            if (!ObjectUtils.isEmpty(compensation.getGratuity()))
                currentCompensation.setGratuity(compensation.getGratuity());
            if (!ObjectUtils.isEmpty(compensation.getPf()))
                currentCompensation.setPf(compensation.getPf());
            if (!ObjectUtils.isEmpty(compensation.getAnnualPackage()))
                currentCompensation.setAnnualPackage(compensation.getAnnualPackage());
            if (!ObjectUtils.isEmpty(compensation.getInsurancePremium()))
                currentCompensation.setInsurancePremium(compensation.getInsurancePremium());
            if (!ObjectUtils.isEmpty(compensation.getGrossPay()))
                currentCompensation.setGrossPay(compensation.getGrossPay());
            if (!ObjectUtils.isEmpty(compensation.getBonuses())) {
                log.debug("Updating {} bonuses", compensation.getBonuses().size());
                currentCompensation.setBonuses(compensation.getBonuses().stream()
                        .map(bonus -> {
                            Bonus bonusEntity = modelMapper.map(bonus, Bonus.class);
                            bonusEntity.setCompensation(currentCompensation);
                            return bonusEntity;
                        }).toList());
            }
            if (!ObjectUtils.isEmpty(compensation.getDeductions())) {
                log.debug("Updating {} deductions", compensation.getDeductions().size());
                currentCompensation.setDeductions(compensation.getDeductions().stream()
                        .map(deductionDto -> {
                            Deduction deductionEntity = modelMapper.map(deductionDto, Deduction.class);
                            deductionEntity.setCompensation(currentCompensation);
                            return deductionEntity;
                        }).toList());
            }
            hrEntity.setCompensation(currentCompensation);
            log.info("✓ Compensation updated successfully");

            // Build PDF template data for revised compensation card
            log.info("Building PDF template data for revised compensation card");
            Position currentPosition = hrEntity.getPositions().getLast();
            PdfTemplateDto pdfTemplateData = buildPdfTemplateData(HrInitRequestDto.builder()
                            .employeeId(hrEntity.getEmployeeId())
                            .department(hrEntity.getDepartment())
                            .title(currentPosition.getTitle())
                            .remarks("Reward Appraisal - Compensation Revision")
                            .compensation(modelMapper.map(compensation, CompensationDto.class)).build(),
                    new Timestamp(System.currentTimeMillis()));

            // Generate revised compensation card asynchronously
            log.info("Starting async document generation: revised compensation card for reward appraisal");
            CompletableFuture<AsyncDocumentService.DocumentResult> revisedCompensationCardFuture = asyncDocumentService
                    .generateAndUploadCompensationCard(pdfTemplateData, hrEntity.getEmployeeId(), hrEntity.getHrId());

            // Wait for async operation to complete
            revisedCompensationCardFuture.join();
            log.info("Document generation and upload task completed for employee: {}", hrEntity.getEmployeeId());

            // Get result
            AsyncDocumentService.DocumentResult revisedCompensationCardResult = revisedCompensationCardFuture.join();

            // Validate revised compensation card
            if (!revisedCompensationCardResult.isSuccess()) {
                ErrorResponseDto error = new ErrorResponseDto();
                error.setMessage("Error uploading Revised Compensation Card to DMS: "
                        + revisedCompensationCardResult.getErrorMessage());
                error.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
                error.setTimestamp(new Timestamp(System.currentTimeMillis()));
                error.setServiceMethod("rewardAppraisal");
                return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            log.info("✓ Revised compensation card generated and uploaded successfully");

            // Get document URL
            String revisedCompensationCardUrl = revisedCompensationCardResult.getDocumentUrl();

            // Add revised compensation card to compensation's card list
            HrDocument revisedCardDocument = revisedCompensationCardResult.toHrDocument(currentCompensation);
            currentCompensation.getCompensationCard().add(revisedCardDocument);

            // Save updated HR entity with all relationships
            log.info("=== Saving updated HrEntity with reward appraisal and new document ===");
            try {
                hrEntityRepo.save(hrEntity);
                log.info("✓✓✓ Successfully saved HrEntity after reward appraisal. HrId: {}", hrEntity.getHrId());
            } catch (Exception saveException) {
                log.error("✗✗✗ FAILED to save HrEntity after reward appraisal. Error: {}", saveException.getMessage());
                log.error("Exception details:", saveException);
                throw saveException;
            }

            // Send reward appraisal email notification
            log.info("=== Sending reward appraisal notification email to employee {} ===", hrEntity.getEmployeeId());
            try {
                // Fetch employee details from user service to get email
                RestPayload restPayload = commonUtils.buildRestPayload(webConstants.getGetUserDetailsUrl(),
                        Map.of("userId", hrEntity.getEmployeeId().toString()), null, "json");
                ResponseEntity<?> userResponse = restServices.hrRestCall(restPayload.getBuilder().toUriString(), null,
                        restPayload.getHeaders(), HttpMethod.GET, hrEntity.getHrId());

                String employeeEmail = null;
                String employeeName = "Employee";

                if (userResponse.getStatusCode().is2xxSuccessful() && userResponse.getBody() != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> userDetails = (Map<String, String>) userResponse.getBody();
                    employeeEmail = userDetails.get("email");
                    String firstName = userDetails.getOrDefault("firstName", "");
                    String lastName = userDetails.getOrDefault("lastName", "");
                    employeeName = (firstName + " " + lastName).trim();
                }

                if (employeeEmail != null) {
                    EmailCommunicationDto emailCommunicationDto = new EmailCommunicationDto();
                    emailCommunicationDto.setSenderEmail("hr@nexus.com");
                    emailCommunicationDto.setRecipientEmails(List.of(employeeEmail));
                    emailCommunicationDto.setSubject("Reward Appraisal - Compensation Revision");
                    emailCommunicationDto.setBody(communicationTemplateBuilder.buildRewardAppraisalEmailTemplate());

                    // Create placeholders map for dynamic content replacement
                    Map<String, Object> placeholders = new HashMap<>();
                    placeholders.put("employeeName", employeeName);
                    placeholders.put("organizationName", "Nexus Corporation");
                    placeholders.put("position", currentPosition.getTitle());
                    placeholders.put("department", hrEntity.getDepartment());
                    placeholders.put("appraisalDate", new java.text.SimpleDateFormat("dd-MM-yyyy")
                            .format(new Timestamp(System.currentTimeMillis())));
                    placeholders.put("effectiveDate", new java.text.SimpleDateFormat("dd-MM-yyyy")
                            .format(new Timestamp(System.currentTimeMillis())));
                    placeholders.put("basePay", String.format("₹%.2f", currentCompensation.getBasePay()));
                    placeholders.put("hra", String.format("₹%.2f", currentCompensation.getHra()));
                    placeholders.put("grossPay", String.format("₹%.2f", currentCompensation.getGrossPay()));
                    placeholders.put("annualPackage",
                            currentCompensation.getAnnualPackage() != null ? currentCompensation.getAnnualPackage()
                                    : String.format("₹%.2f", currentCompensation.getGrossPay() * 12));
                    placeholders.put("hrEmail", "hr@nexus.com");
                    emailCommunicationDto.setPlaceholders(placeholders);

                    // Set attachment with revised compensation card
                    emailCommunicationDto.setAttachments(List.of(
                            new EmailAttachmentDto("Revised_Compensation_Card_" + hrEntity.getEmployeeId() + ".pdf",
                                    "application/pdf", revisedCompensationCardUrl)));

                    communicationService.sendCommunicationOverEmail(emailCommunicationDto);
                    log.info("✓ Reward appraisal notification email sent successfully to employee: {}",
                            hrEntity.getEmployeeId());
                } else {
                    log.warn("Could not fetch employee email from user service for employee ID: {}",
                            hrEntity.getEmployeeId());
                }
            } catch (Exception emailException) {
                // Log the email error but don't throw exception - prevents transaction rollback
                log.error(
                        "Email sending failed but reward appraisal was processed successfully. Employee ID: {}, Error: {}",
                        hrEntity.getEmployeeId(), emailException.getMessage(), emailException);
            }

            // Build response with appraisal details and document URL
            GeneratedPdfDto appraisalResponse = GeneratedPdfDto.builder()
                    .hrId(hrEntity.getHrId())
                    .employeeId(hrEntity.getEmployeeId())
                    .documentType("REWARD_APPRAISAL")
                    .revisedCompensationCardUrl(revisedCompensationCardUrl)
                    .newPosition(currentPosition.getTitle())
                    .basePay(currentCompensation.getBasePay())
                    .hra(currentCompensation.getHra())
                    .netPay(currentCompensation.getNetPay())
                    .annualPackage(currentCompensation.getAnnualPackage() != null
                            ? currentCompensation.getAnnualPackage()
                            : String.format("₹%.2f", currentCompensation.getGrossPay() * 12))
                    .generatedAt(new Timestamp(System.currentTimeMillis()))
                    .build();

            log.info("✓ Reward appraisal completed successfully for employee: {}", hrEntity.getEmployeeId());

            return ResponseEntity.ok(appraisalResponse);

        } catch (ResourceNotFoundException | ServiceLevelException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceLevelException("HR Service", "Exception occurred while processing reward appraisal",
                    "rewardAppraisal", e.getClass().getName(), e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getEmployeesOnNoticePeriod(Long orgId) {
        if (ObjectUtils.isEmpty(orgId)) {
            throw new ServiceLevelException("HR Service", "Organization ID cannot be null or empty",
                    "getEmployeesOnNoticePeriod", "InvalidInput", "Organization ID is null or empty");
        }
        ResponseEntity<?> response;
        try {
            Integer allWhoAreOnNoticePeriod = hrEntityRepo.getAllWhoAreOnNoticePeriod(orgId);
            response = ResponseEntity.ok(allWhoAreOnNoticePeriod);
        } catch (Exception e) {
            throw new ServiceLevelException("HR Service", "Exception occurred while fetching employees on notice period",
                    "getEmployeesOnNoticePeriod", e.getClass().getName(), e.getMessage());
        }
        return response;
    }

    @Override
    public ResponseEntity<?> getEmployeesDirectory(List<Long> empIds) {
        if (ObjectUtils.isEmpty(empIds)) {
            throw new ServiceLevelException("HR Service", "Employee IDs list cannot be null or empty",
                    "getEmployeesDirectory", "InvalidInput", "Employee IDs list is null or empty");
        }
        ResponseEntity<?> response;
        try {
            List<EmployeeDirectoryResponse> list = empIds.stream().map(id -> {
                HrEntity hrEntity = hrEntityRepo.findByEmployeeId(id).orElseThrow(() -> new ResourceNotFoundException("HrEntity", "employeeId", id));
                return new EmployeeDirectoryResponse(hrEntity.getEmployeeId(), hrEntity.getDepartment(), hrEntity.getPositions().getLast().getTitle(), hrEntity.getCompensation().getNetPay(), hrEntity.getDateOfJoining());
            }).toList();
            response = ResponseEntity.ok(list);
        } catch (RuntimeException e) {
            throw new ServiceLevelException("HR Service", "Exception occurred while fetching employees directory",
                    "getEmployeesDirectory", e.getClass().getName(), e.getMessage());
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
                    .grossPay(hrInitRequestDto.getCompensation().getGrossPay());
        }

        return builder.build();
    }
}
