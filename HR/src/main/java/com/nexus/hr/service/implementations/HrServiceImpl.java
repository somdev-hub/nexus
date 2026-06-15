package com.nexus.hr.service.implementations;

import com.nexus.hr.exception.ResourceNotFoundException;
import com.nexus.hr.exception.ServiceLevelException;
import com.nexus.hr.model.entities.*;
import com.nexus.hr.model.enums.AttendanceStatus;
import com.nexus.hr.model.enums.HrRequestStatus;
import com.nexus.hr.model.enums.PaymentStatus;
import com.nexus.hr.payload.*;
import com.nexus.hr.payload.response.EmployeeDetailsResponse;
import com.nexus.hr.payload.response.EmployeeDirectoryResponse;
import com.nexus.hr.payload.response.PayrollEmployeeResponse;
import com.nexus.hr.repository.HrEntityRepo;
import com.nexus.hr.repository.HrRequestRepo;
import com.nexus.hr.repository.PayrollRepo;
import com.nexus.hr.repository.PositionRepository;
import com.nexus.hr.service.interfaces.CommsService;
import com.nexus.hr.service.interfaces.CommunicationService;
import com.nexus.hr.service.interfaces.HrService;
import com.nexus.hr.utils.*;
import com.nexus.hr.views.CommunicationTemplateBuilder;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

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
    private final PositionRepository positionRepository;
    private final PayrollRepo payrollRepo;
    private final CommsService commsService;

    private static @NonNull AttendanceStatus getAttendanceStatus(TimeManagement attendance) {
        AttendanceStatus status;
        switch (attendance) {
            case TimeManagement tm when tm.getIsPresent() && tm.getIsHalfDay() -> status = AttendanceStatus.HALF_DAY;
            case TimeManagement tm when tm.getIsPresent() -> status = AttendanceStatus.PRESENT;
            case TimeManagement tm when tm.getIsOnLeave() -> status = AttendanceStatus.ON_LEAVE;
            default -> status = AttendanceStatus.ABSENT;
        }
        return status;
    }

    @Override
    public ResponseEntity<?> initHr(HrInitRequestDto hrInitRequestDto) {
        ResponseEntity<?> response = null;
        try {
            // CRITICAL: Transactional part - only save to database
            HrEntity savedHrEntity = saveHrEntityTransaction(hrInitRequestDto);

            // Generate documents and get URLs (this will wait for document generation to
            // complete)
            DocumentUrls documentUrls = getDocumentUrls(hrInitRequestDto, savedHrEntity);

            // Fire remaining async operations in background (OUTSIDE transaction)
            // This prevents connection leaks and blocking on Kafka operations
            CompletableFuture.runAsync(() -> {
                try {
                    processAsyncHrOperations(hrInitRequestDto, savedHrEntity, documentUrls);
                } catch (Exception e) {
                    log.error("Error processing async HR operations for employee: {}",
                            savedHrEntity.getEmployeeId(), e);
                }
            });

            // Return successful response with document URLs
            response = ResponseEntity.ok(HrInitResponse.builder()
                    .hrId(savedHrEntity.getHrId())
                    .joiningLetterUrl(documentUrls.joiningLetterUrl())
                    .letterOfIntentUrl(documentUrls.letterOfIntentUrl())
                    .compensationCardUrl(documentUrls.compensationCardUrl())
                    .build());

        } catch (Exception e) {
            throw new ServiceLevelException("HR Service", "Exception occurred while initializing HR module", "initHr",
                    e.getClass().getName(), e.getMessage());
        }

        return response;
    }

    /**
     * Generate documents and return their URLs
     */
    private DocumentUrls getDocumentUrls(HrInitRequestDto hrInitRequestDto, HrEntity savedHrEntity) {
        // Generate PDF template data
        PdfTemplateDto pdfTemplateData = buildPdfTemplateData(hrInitRequestDto,
                new Timestamp(savedHrEntity.getDateOfJoining().getTime()));

        // Start async PDF generation and DMS uploads in parallel
        log.info("Starting parallel document generation for employee: {}", savedHrEntity.getEmployeeId());
        CompletableFuture<AsyncDocumentService.DocumentResult> joiningLetterFuture = asyncDocumentService
                .generateAndUploadJoiningLetter(pdfTemplateData, savedHrEntity.getEmployeeId(),
                        savedHrEntity.getHrId());

        CompletableFuture<AsyncDocumentService.DocumentResult> letterOfIntentFuture = asyncDocumentService
                .generateAndUploadLetterOfIntent(pdfTemplateData, savedHrEntity.getEmployeeId(),
                        savedHrEntity.getHrId());

        CompletableFuture<AsyncDocumentService.DocumentResult> compensationCardFuture = asyncDocumentService
                .generateAndUploadCompensationCard(pdfTemplateData, savedHrEntity.getEmployeeId(),
                        savedHrEntity.getHrId());

        // Wait for all document generation to complete
        CompletableFuture.allOf(joiningLetterFuture, letterOfIntentFuture, compensationCardFuture).join();
        log.info("All document generation tasks completed for employee: {}", savedHrEntity.getEmployeeId());

        // Get results
        AsyncDocumentService.DocumentResult joiningLetterResult = joiningLetterFuture.join();
        AsyncDocumentService.DocumentResult letterOfIntentResult = letterOfIntentFuture.join();
        AsyncDocumentService.DocumentResult compensationCardResult = compensationCardFuture.join();

        // Log results
        if (!joiningLetterResult.isSuccess()) {
            log.error("Error uploading Joining Letter to DMS: {}", joiningLetterResult.getErrorMessage());
        }
        if (!letterOfIntentResult.isSuccess()) {
            log.error("Error uploading Letter of Intent to DMS: {}", letterOfIntentResult.getErrorMessage());
        }
        if (!compensationCardResult.isSuccess()) {
            log.error("Error uploading Compensation Card to DMS: {}", compensationCardResult.getErrorMessage());
        }

        // Extract URLs from results
        String joiningLetterUrl = joiningLetterResult.isSuccess() ? joiningLetterResult.getDocumentUrl() : "";
        String letterOfIntentUrl = letterOfIntentResult.isSuccess() ? letterOfIntentResult.getDocumentUrl() : "";
        String compensationCardUrl = compensationCardResult.isSuccess() ? compensationCardResult.getDocumentUrl() : "";

        return new DocumentUrls(joiningLetterUrl, letterOfIntentUrl, compensationCardUrl);
    }

    /**
     * CRITICAL: Transactional method that ONLY saves HR entity to database
     * Does NOT block on async operations - fixes HikariCP connection leak
     */
    @Transactional
    protected HrEntity saveHrEntityTransaction(HrInitRequestDto hrInitRequestDto) {
        HrEntity hrEntity = new HrEntity();
        hrEntity.setEmployeeId(hrInitRequestDto.getEmployeeId());
        hrEntity.setOrg(hrInitRequestDto.getOrgId());
        hrEntity.setDepartment(hrInitRequestDto.getDepartment());
        hrEntity.setDateOfJoining(Date.valueOf(LocalDate.now()));
        hrEntity.setIsActive(Boolean.TRUE);

        Position position = new Position();
        position.setTitle(hrInitRequestDto.getTitle());
        position.setDepartment(hrInitRequestDto.getDepartment());
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

        log.info("=== Attempting to save HrEntity for employeeId: {} ===", hrInitRequestDto.getEmployeeId());
        log.debug("HrEntity details: org={}, department={}, positions count={}, documents count={}",
                hrEntity.getOrg(), hrEntity.getDepartment(),
                hrEntity.getPositions().size(),
                hrEntity.getHrDocuments() != null ? hrEntity.getHrDocuments().size() : 0);

        HrEntity savedHrEntity = hrEntityRepo.save(hrEntity);
        log.info("✓ Successfully saved HrEntity with hrId: {}", savedHrEntity.getHrId());

        return savedHrEntity;
    }

    /**
     * Save HrEntity with all relationships (compensation, leaves, documents) in a
     * transactional context
     * This ensures proper handling of collections and prevents duplicate inserts
     */
    @Transactional
    protected void saveHrEntityWithRelationships(HrEntity hrEntity) {
        try {
            hrEntityRepo.save(hrEntity);
            log.info("✓✓✓ Successfully saved HrEntity with all relationships. HrId: {}", hrEntity.getHrId());
        } catch (Exception saveException) {
            log.error("✗✗✗ FAILED to save HrEntity with relationships. Error: {}", saveException.getMessage(),
                    saveException);
            throw new ServiceLevelException("HR Service",
                    "Exception occurred while saving HR entity with relationships",
                    "saveHrEntityWithRelationships",
                    saveException.getClass().getName(),
                    saveException.getMessage());
        }
    }

    /**
     * CRITICAL: Non-transactional method that handles async operations
     * This runs AFTER the database transaction commits
     * Prevents connection leaks and allows Kafka publishing to complete
     */
    private void processAsyncHrOperations(HrInitRequestDto hrInitRequestDto, HrEntity savedHrEntity,
                                          DocumentUrls documentUrls) {
        // Get extracted URLs
        String joiningLetterUrl = documentUrls.joiningLetterUrl();
        String letterOfIntentUrl = documentUrls.letterOfIntentUrl();
        String compensationCardUrl = documentUrls.compensationCardUrl();

        try {
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
                        log.debug("BankRecord: bank={}, accountType={}, hasAccountNumber={}, hasIfsc={}",
                                bankRecord.getBankName() != null ? "***" : "null",
                                bankRecord.getAccountType(),
                                bankRecord.getAccountNumber() != null,
                                bankRecord.getIfscCode() != null);
                        bankRecord.setCompensation(compensation);
                        return bankRecord;
                    }).toList());
            log.info("✓ Compensation entity built successfully");

            // Add compensation card document if URL is available
            if (!compensationCardUrl.isEmpty()) {
                // Note: We need to fetch the document result to create HrDocument
                // For now, we'll create a basic HrDocument from the URL
                HrDocument compensationCardDoc = new HrDocument();
                compensationCardDoc.setDocumentName("Compensation_Card_" + savedHrEntity.getEmployeeId() + ".pdf");
                compensationCardDoc.setDocumentUrl(compensationCardUrl);
                compensationCardDoc.setHrEntity(savedHrEntity);
                compensationCardDoc.setHrDocumentType("COMPENSATION_CARD");
                compensation.getCompensationCard().add(compensationCardDoc);
            }

            // Link compensation to hrEntity
            compensation.setHrEntity(savedHrEntity);
            savedHrEntity.setCompensation(compensation);

            // Send communication email and Kafka message
            try {
//                EmailCommunicationDto emailCommunicationDto = new EmailCommunicationDto();
//                emailCommunicationDto.setSenderEmail("hr@nexus.com");
//                emailCommunicationDto.setRecipientEmails(List.of(hrInitRequestDto.getPersonalEmail()));
//                emailCommunicationDto.setSubject("Update on your application");
//                emailCommunicationDto.setBody(communicationTemplateBuilder.buildHrInitEmailTemplate());
//
//                Map<String, Object> placeholders = new HashMap<>();
//                placeholders.put("name", hrInitRequestDto.getFullName());
//                placeholders.put("employeeId", savedHrEntity.getEmployeeId());
//                placeholders.put("department", hrInitRequestDto.getDepartment());
//                placeholders.put("position", hrInitRequestDto.getTitle());
//                placeholders.put("dateOfJoining", savedHrEntity.getDateOfJoining().toString());
//                placeholders.put("organizationName", "Nexus Corporation");
//                emailCommunicationDto.setPlaceholders(placeholders);
//
//                emailCommunicationDto.setAttachments(List.of(
//                        new EmailAttachmentDto("Joining_Letter_" + savedHrEntity.getEmployeeId() + ".pdf",
//                                "application/pdf", joiningLetterUrl),
//                        new EmailAttachmentDto("Letter_Of_Intent_" + savedHrEntity.getEmployeeId() + ".pdf",
//                                "application/pdf", letterOfIntentUrl),
//                        new EmailAttachmentDto("Compensation_Card_" + savedHrEntity.getEmployeeId() + ".pdf",
//                                "application/pdf", compensationCardUrl)));
//
//                // CRITICAL: Publish to Kafka (now outside transaction)
//                communicationService.sendCommunicationOverKafkaForCandidateSelection(emailCommunicationDto);


                List<EmailAttachmentDto> emailAttachmentDtos = List.of(
                        new EmailAttachmentDto("Joining_Letter_" + savedHrEntity.getEmployeeId() + ".pdf",
                                "application/pdf", joiningLetterUrl),
                        new EmailAttachmentDto("Letter_Of_Intent_" + savedHrEntity.getEmployeeId() + ".pdf",
                                "application/pdf", letterOfIntentUrl),
                        new EmailAttachmentDto("Compensation_Card_" + savedHrEntity.getEmployeeId() + ".pdf",
                                "application/pdf", compensationCardUrl));
                commsService.sendCommunication(CommonConstants.CommsTriggerPoint.CANDIDATE_SELECTION_MAIL, savedHrEntity.getHrId(), emailAttachmentDtos);

                log.info("Welcome email published to Kafka successfully for employee ID: {}",
                        savedHrEntity.getEmployeeId());
            } catch (Exception emailException) {
                log.error("Error sending Kafka message for employee: {}. Error: {}",
                        savedHrEntity.getEmployeeId(), emailException.getMessage(), emailException);
            }

            // Reload entity from database to ensure we have the latest state and avoid
            // detached entity issues
            log.info("=== Reloading HrEntity from database before leave allocation ===");
            HrEntity refreshedHrEntity = hrEntityRepo.findById(savedHrEntity.getHrId())
                    .orElseThrow(() -> new ResourceNotFoundException("HrEntity", "hrId", savedHrEntity.getHrId()));

            // Initialize leave allocations
            log.info("=== Initializing leave allocations for employee: {} ===", refreshedHrEntity.getEmployeeId());
            leaveAllocationUtils.initializeLeaveAllocations(refreshedHrEntity);
            log.info("✓ Leave allocations initialized successfully");

            // Save with all relationships in a transactional context
            log.info("=== Attempting save of HrEntity with compensation and leave allocations ===");
            saveHrEntityWithRelationships(refreshedHrEntity);
        } catch (Exception e) {
            log.error("Error processing async HR operations for employee: {}",
                    savedHrEntity.getEmployeeId(), e);
        }
    }

    @Override
    public ResponseEntity<?> promoteEmployee(Long empId, Position position, CompensationDto compensation, String role) {
        if (ObjectUtils.isEmpty(empId) || ObjectUtils.isEmpty(position) || ObjectUtils.isEmpty(compensation)
                || ObjectUtils.isEmpty(role)) {
            throw new ServiceLevelException("HR Service", "HR ID, Position, and Compensation cannot be null or empty",
                    "promoteEmployee", "InvalidInput", "One or more inputs are null or empty");
        }
        try {
            log.info("=== Starting promotion process for hrId: {} ===", empId);

            HrEntity hrEntity = hrEntityRepo.findByEmployeeId(empId)
                    .orElseThrow(() -> new ResourceNotFoundException("HrEntity", "empId", empId));

            // Deactivate last position and set end date
            log.info("Deactivating previous position for employee: {}", hrEntity.getEmployeeId());
            Position lastPosition = hrEntity.getPositions().getLast();
            lastPosition.setIsActive(Boolean.FALSE);
            lastPosition.setLastEffectiveDate(new Timestamp(System.currentTimeMillis()));

            // Activate new position
            position.setIsActive(Boolean.TRUE);
            position.setHrEntity(hrEntity);
            position.setEffectiveFrom(new Timestamp(System.currentTimeMillis()));
            positionRepository.save(position);
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
                RestPayload restPayload = commonUtils.buildRestPayload(webConstants.getUserDetailsUrl(),
                        Map.of("userId", hrEntity.getEmployeeId().toString()), null, CommonConstants.APPLICATION_JSON);
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
                RestPayload restPayload = commonUtils.buildRestPayload(webConstants.getUserDetailsUrl(),
                        Map.of("userId", hrEntity.getEmployeeId().toString()), null, CommonConstants.APPLICATION_JSON);
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
            throw new ServiceLevelException("HR Service",
                    "Exception occurred while fetching employees on notice period",
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
                HrEntity hrEntity = hrEntityRepo.findByEmployeeId(id)
                        .orElseThrow(() -> new ResourceNotFoundException("HrEntity", "employeeId", id));
                return new EmployeeDirectoryResponse(hrEntity.getEmployeeId(), hrEntity.getDepartment(),
                        hrEntity.getPositions().getLast().getTitle(), hrEntity.getCompensation().getNetPay(),
                        hrEntity.getDateOfJoining());
            }).toList();
            response = ResponseEntity.ok(list);
        } catch (RuntimeException e) {
            throw new ServiceLevelException("HR Service", "Exception occurred while fetching employees directory",
                    "getEmployeesDirectory", e.getClass().getName(), e.getMessage());
        }
        return response;
    }

    @Override
    public ResponseEntity<?> getEmployeeDetails(Long empId) {
        if (ObjectUtils.isEmpty(empId)) {
            throw new ServiceLevelException("HR Service", "Employee ID cannot be null or empty",
                    "getEmployeeDetails", "InvalidInput", "Employee ID is null or empty");
        }
        ResponseEntity<?> response = null;
        try {
            HrEntity hrEntity = hrEntityRepo.findByEmployeeId(empId)
                    .orElseThrow(() -> new ResourceNotFoundException("HrEntity", "employeeId", empId));
            EmployeeDetailsResponse employeeDetailsResponse = new EmployeeDetailsResponse();
            employeeDetailsResponse.setDepartment(hrEntity.getDepartment());
            employeeDetailsResponse.setJobTitle(hrEntity.getPositions().getLast().getTitle());
            employeeDetailsResponse
                    .setJoiningDate(LocalDateTime.of(hrEntity.getDateOfJoining().toLocalDate(), LocalTime.MIDNIGHT));
            employeeDetailsResponse.setAnnualSalary(hrEntity.getCompensation().getNetPay()); // later to be changed to
            // gross pay
            List<EmployeeDetailsResponse.PositionsHeld> positionsHeld = hrEntity.getPositions().stream()
                    .map(position -> {
                        Double duration = position.getLastEffectiveDate() != null
                                ? (position.getLastEffectiveDate().getTime() - position.getEffectiveFrom().getTime())
                                / (1000.0 * 60 * 60 * 24 * 30)
                                : (System.currentTimeMillis() - position.getEffectiveFrom().getTime())
                                / (1000.0 * 60 * 60 * 24 * 30);
                        return new EmployeeDetailsResponse.PositionsHeld(position.getTitle(), position.getDepartment(),
                                position.getEffectiveFrom(), position.getLastEffectiveDate(), duration);
                    }).toList();
            employeeDetailsResponse.setPositionsHeld(positionsHeld);
            employeeDetailsResponse.setCompensation(modelMapper.map(hrEntity.getCompensation(), CompensationDto.class));
            List<EmployeeDetailsResponse.HrDocuments> hrDocuments = hrEntity.getHrDocuments().stream()
                    .map(document -> new EmployeeDetailsResponse.HrDocuments(document.getDocumentName(),
                            document.getDocumentUrl(), document.getCreatedOn(), document.getHrDocumentType()))
                    .toList();
            employeeDetailsResponse.setHrDocuments(hrDocuments);
            // attendance
            List<EmployeeDetailsResponse.AttendanceRecord> attendanceRecords = hrEntity.getTimeManagements().stream()
                    .map(attendance -> {
                        // Date date =
                        // Date.valueOf(attendance.getCreatedOn().toLocalDateTime().toLocalDate());
                        LocalDateTime datetime = LocalDateTime
                                .of(attendance.getCreatedOn().toLocalDateTime().toLocalDate(), LocalTime.MIDNIGHT);
                        // decide status
                        AttendanceStatus status = getAttendanceStatus(attendance);
                        Double totalBreakHours = attendance.getBreakEndTime() != null
                                && attendance.getBreakStartTime() != null
                                ? (attendance.getBreakEndTime().getTime()
                                - attendance.getBreakStartTime().getTime()) / (1000.0 * 60 * 60)
                                : 0.0;
                        return new EmployeeDetailsResponse.AttendanceRecord(datetime, status,
                                attendance.getCheckInTime(), attendance.getCheckOutTime(),
                                attendance.getTotalHoursWorked(), totalBreakHours, attendance.getOvertimeHours());
                    }).toList();
            List<EmployeeDetailsResponse.LeaveRecord> leaveRecords = hrEntity.getLeaveAllocations().stream()
                    .map(leave -> new EmployeeDetailsResponse.LeaveRecord(leave.getLeaveType().name(),
                            leave.getAllocatedDays(), leave.getUsedDays(), leave.getRemainingDays()))
                    .toList();
            employeeDetailsResponse.setAttendanceRecords(attendanceRecords);
            employeeDetailsResponse.setLeaveRecords(leaveRecords);
            response = ResponseEntity.ok(employeeDetailsResponse);
        } catch (RuntimeException e) {
            throw new ServiceLevelException("HR Service", "Exception occurred while fetching employee details",
                    "getEmployeeDetails", e.getClass().getName(), e.getMessage());
        }
        return response;
    }

    @Override
    public ResponseEntity<?> getPayrollEmployees(List<Long> empIds) {
        if (ObjectUtils.isEmpty(empIds) || empIds.isEmpty()) {
            throw new ServiceLevelException("HR Service", "Employee IDs list cannot be null or empty",
                    "getPayrollEmployees", "InvalidInput", "Employee IDs list is null or empty");
        }
        try {
            log.info("=== Fetching payroll employees for {} employees ===", empIds.size());

            // Get current month and year
            java.time.YearMonth currentMonth = java.time.YearMonth.now();
            String monthName = currentMonth.getMonth().name();
            Integer year = currentMonth.getYear();

            log.debug("Looking for payroll records for month: {}, year: {}", monthName, year);

            // Build payroll employee response list
            List<PayrollEmployeeResponse> payrollEmployees = empIds.stream().map(empId -> {
                HrEntity hrEntity = hrEntityRepo.findByEmployeeId(empId)
                        .orElseThrow(() -> new ResourceNotFoundException("HrEntity", "employeeId", empId));

                // Get current position (title)
                Position currentPosition = hrEntity.getPositions().getLast();
                String positionTitle = currentPosition.getTitle();

                // Get department
                String department = hrEntity.getDepartment();

                // Get compensation details
                Compensation compensation = hrEntity.getCompensation();
                Double monthlySalaryGross = compensation.getGrossPay();
                Double monthlySalaryNet = compensation.getNetPay();

                // Check for payroll record in current month
                List<Payroll> payrollOptional = payrollRepo.findByCompensationIdAndMonthAndYear(
                        compensation.getCompensationId(), monthName, year);

                PayrollEmployeeResponse.PayrollEmployeeResponseBuilder responseBuilder = PayrollEmployeeResponse.builder()
                        .employeeId(empId)
                        .positionTitle(positionTitle)
                        .department(department)
                        .monthlySalaryGross(monthlySalaryGross)
                        .monthlySalaryNet(monthlySalaryNet);

                // If payroll exists for current month, include payment details
                if (!payrollOptional.isEmpty()) {
                    Payroll payroll = payrollOptional.getFirst();
                    responseBuilder.paymentStatus(payroll.getPaymentStatus())
                            .month(payroll.getMonth())
                            .year(payroll.getYear())
                            .paidOn(payroll.getPaidOn())
                            .paymentReferenceId(payroll.getPaymentReferenceId());
                    log.debug("Found payroll record for employee {} with status: {}", empId, payroll.getPaymentStatus());
                } else {
                    // No payroll record for current month
                    responseBuilder.paymentStatus(PaymentStatus.NOT_PROCESSED)
                            .month(monthName)
                            .year(year);
                    log.debug("No payroll record found for employee {} in current month", empId);
                }

                return responseBuilder.build();
            }).toList();

            return ResponseEntity.ok(payrollEmployees);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceLevelException("HR Service", "Exception occurred while fetching payroll employees",
                    "getPayrollEmployees", e.getClass().getName(), e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getPayrollProcessed(Long orgId, Integer month, Integer year, Integer pageNo, Integer pageSize) {
        if (ObjectUtils.isEmpty(orgId) || ObjectUtils.isEmpty(month) || ObjectUtils.isEmpty(year) ||
                ObjectUtils.isEmpty(pageNo) || ObjectUtils.isEmpty(pageSize)) {
            throw new ServiceLevelException("HR Service",
                    "Organization ID, month, year, page number, and page size cannot be null or empty",
                    "getPayrollProcessed", "InvalidInput", "One or more inputs are null or empty");
        }

        try {
            log.info("=== Fetching processed payroll records for orgId: {}, month: {}, year: {}, page: {}, size: {} ===",
                    orgId, month, year, pageNo, pageSize);

            // Convert month number to month name
            String monthName = java.time.Month.of(month).name();

            // Create pageable with pagination and sort by paidOn descending
            Pageable pageable = PageRequest.of(pageNo, pageSize,
                    Sort.by(
                            Sort.Order.desc("paidOn")));

            // Fetch paginated processed payroll records
            Page<Payroll> payrollPage = payrollRepo.findPayrollsByOrgIdAndMonthAndYearAndProcessedStatus(
                    orgId, monthName, year, pageable);

            log.info("Successfully retrieved processed payroll records - Total elements: {}, Total pages: {}, Current page: {}",
                    payrollPage.getTotalElements(), payrollPage.getTotalPages(), payrollPage.getNumber());

            List<Map<String,Object>> payrollList = payrollPage.stream().map(payroll -> {
                Compensation compensation = payroll.getCompensation();
                Map<String, Object> compensationMap = new ConcurrentHashMap<>();
                compensationMap.put("basePay", calculateMonthly(compensation.getBasePay()));
                compensationMap.put("hra", calculateMonthly(compensation.getHra()));
                compensationMap.put("grossPay", calculateMonthly(compensation.getGrossPay()));
                compensationMap.put("totalBonuses", calculateMonthlyTotalBonuses(compensation.getBonuses()));
                compensationMap.put("totalDeductions", calculateMonthlyTotalDeductions(compensation.getDeductions()));
                compensationMap.put("totalOvertimeFee", calculateMonthlyOvertimeFee(compensation));
                compensationMap.put("empId", compensation.getHrEntity().getEmployeeId());
                compensationMap.put("netPay",  calculateMonthlyNetPay(compensation));
                return compensationMap;
            }).toList();

            return ResponseEntity.ok(payrollList);
        } catch (Exception e) {
            log.error("Error fetching processed payroll records for orgId: {}", orgId, e);
            throw new ServiceLevelException("HR Service",
                    "Exception occurred while fetching processed payroll records", "getPayrollProcessed",
                    e.getClass().getName(), e.getMessage());
        }
    }

    private Double calculateMonthlyNetPay(Compensation compensation) {
        Double netPay = compensation.getNetPay();
        return netPay != null ? (netPay / 12) + calculateMonthlyOvertimeFee(compensation) : null;
    }

    private Double calculateMonthlyOvertimeFee(Compensation compensation) {
        return compensation.getHrEntity().getTimeManagements().stream()
                .filter(attendance -> attendance.getOvertimeHours() != null && attendance.getOvertimeHours() > 0)
                .mapToDouble(attendance -> {
                    Double overtimeRate = compensation.getGrossPay() / (30 * 8); // Assuming 30 days and 8 hours/day
                    return attendance.getOvertimeHours() * overtimeRate;
                }).sum();
    }

    private Double calculateMonthlyTotalDeductions(List<Deduction> deductions) {
        if (deductions == null || deductions.isEmpty()) {
            return null;
        }
        Double totalDeductions = deductions.stream()
                .filter(deduction -> deduction.getAmount() != null)
                .mapToDouble(Deduction::getAmount)
                .sum();
        return totalDeductions / 12;
    }

    private Double calculateMonthlyTotalBonuses(List<Bonus> bonuses) {
        if (bonuses == null || bonuses.isEmpty()) {
            return null;
        }
        Double totalBonuses = bonuses.stream()
                .filter(bonus -> bonus.getAmount() != null)
                .mapToDouble(Bonus::getAmount)
                .sum();
        return totalBonuses / 12;
    }

    private Double calculateMonthly(Double basePay) {
        if (basePay == null) {
            return null;
        }
        return basePay / 12;
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

    /**
     * Helper class to hold document URLs
     */
    private record DocumentUrls(String joiningLetterUrl, String letterOfIntentUrl, String compensationCardUrl) {
    }
}
