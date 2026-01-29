package com.nexus.hr.service.implementations;

import com.nexus.hr.model.entities.Compensation;
import com.nexus.hr.model.entities.HrDocument;
import com.nexus.hr.model.entities.Position;
import com.nexus.hr.payload.PdfTemplateDto;
import com.nexus.hr.utils.CommonConstants;
import com.nexus.hr.utils.CommonUtils;
import com.nexus.hr.utils.WebConstants;
import com.nexus.hr.utils.RestServices;
import com.nexus.hr.views.PdfGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Async service for handling document generation and DMS uploads
 * Processes PDF generation and uploads in parallel to optimize performance
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncDocumentService {

    private final PdfGeneratorService pdfGeneratorService;
    private final CommonUtils commonUtils;
    private final WebConstants webConstants;
    private final RestServices restServices;

    /**
     * Result wrapper for document processing
     */
    public static class DocumentResult {
        private final String documentUrl;
        private final String documentName;
        private final String documentType;
        private final boolean success;
        private final String errorMessage;

        public DocumentResult(String documentUrl, String documentName, String documentType, boolean success, String errorMessage) {
            this.documentUrl = documentUrl;
            this.documentName = documentName;
            this.documentType = documentType;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public String getDocumentUrl() {
            return documentUrl;
        }

        public String getDocumentName() {
            return documentName;
        }

        public String getDocumentType() {
            return documentType;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public HrDocument toHrDocument(Position position) {
            HrDocument hrDocument = new HrDocument();
            hrDocument.setDocumentUrl(documentUrl);
            hrDocument.setDocumentName(documentName);
            hrDocument.setHrDocumentType(documentType);
            hrDocument.setPosition(position);
            return hrDocument;
        }

        public HrDocument toHrDocument(Compensation compensation) {
            HrDocument hrDocument = new HrDocument();
            hrDocument.setDocumentUrl(documentUrl);
            hrDocument.setDocumentName(documentName);
            hrDocument.setHrDocumentType(documentType);
            hrDocument.setCompensation(compensation);
            return hrDocument;
        }
    }

    /**
     * Asynchronously generates joining letter PDF and uploads to DMS
     */
    @Async("hrDocumentTaskExecutor")
    public CompletableFuture<DocumentResult> generateAndUploadJoiningLetter(
            PdfTemplateDto pdfTemplateData, Long employeeId, Long hrId) {

        log.info("Starting async generation of Joining Letter for employee: {}", employeeId);

        try {
            // Generate PDF
            MultipartFile joiningLetterPdf = pdfGeneratorService.generateJoiningLetterPdf(pdfTemplateData);

            // Upload to DMS
            String fileName = "Joining_Letter_" + employeeId + ".pdf";
            ResponseEntity<?> dmsResponse = callDmsToUpload(
                joiningLetterPdf, employeeId, fileName, "JOINING_LETTER", hrId);

            // Process response
            if (dmsResponse.getStatusCode().is2xxSuccessful()) {
                @SuppressWarnings("unchecked")
                Map<String, String> responseBody = (Map<String, String>) dmsResponse.getBody();
                if (responseBody != null && responseBody.containsKey("documentUrl")) {
                    log.info("Successfully generated and uploaded Joining Letter for employee: {}", employeeId);
                    return CompletableFuture.completedFuture(new DocumentResult(
                        responseBody.get("documentUrl"),
                        responseBody.get("documentName"),
                        responseBody.get("documentType"),
                        true,
                        null
                    ));
                }
            }

            log.error("Failed to upload Joining Letter to DMS for employee: {}", employeeId);
            return CompletableFuture.completedFuture(new DocumentResult(
                null, null, "JOINING_LETTER", false,
                "DMS upload failed with status: " + dmsResponse.getStatusCode()
            ));

        } catch (Exception e) {
            log.error("Error generating/uploading Joining Letter for employee: {}", employeeId, e);
            return CompletableFuture.completedFuture(new DocumentResult(
                null, null, "JOINING_LETTER", false,
                "Exception: " + e.getMessage()
            ));
        }
    }

    /**
     * Asynchronously generates letter of intent PDF and uploads to DMS
     */
    @Async("hrDocumentTaskExecutor")
    public CompletableFuture<DocumentResult> generateAndUploadLetterOfIntent(
            PdfTemplateDto pdfTemplateData, Long employeeId, Long hrId) {

        log.info("Starting async generation of Letter of Intent for employee: {}", employeeId);

        try {
            // Generate PDF
            MultipartFile letterOfIntentPdf = pdfGeneratorService.generateLetterOfIntentPdf(pdfTemplateData);

            // Upload to DMS
            String fileName = "Letter_Of_Intent_" + employeeId + ".pdf";
            ResponseEntity<?> dmsResponse = callDmsToUpload(
                letterOfIntentPdf, employeeId, fileName, "LETTER_OF_INTENT", hrId);

            // Process response
            if (dmsResponse.getStatusCode().is2xxSuccessful()) {
                @SuppressWarnings("unchecked")
                Map<String, String> responseBody = (Map<String, String>) dmsResponse.getBody();
                if (responseBody != null && responseBody.containsKey("documentUrl")) {
                    log.info("Successfully generated and uploaded Letter of Intent for employee: {}", employeeId);
                    return CompletableFuture.completedFuture(new DocumentResult(
                        responseBody.get("documentUrl"),
                        responseBody.get("documentName"),
                        responseBody.get("documentType"),
                        true,
                        null
                    ));
                }
            }

            log.error("Failed to upload Letter of Intent to DMS for employee: {}", employeeId);
            return CompletableFuture.completedFuture(new DocumentResult(
                null, null, "LETTER_OF_INTENT", false,
                "DMS upload failed with status: " + dmsResponse.getStatusCode()
            ));

        } catch (Exception e) {
            log.error("Error generating/uploading Letter of Intent for employee: {}", employeeId, e);
            return CompletableFuture.completedFuture(new DocumentResult(
                null, null, "LETTER_OF_INTENT", false,
                "Exception: " + e.getMessage()
            ));
        }
    }

    /**
     * Asynchronously generates compensation card PDF and uploads to DMS
     */
    @Async("hrDocumentTaskExecutor")
    public CompletableFuture<DocumentResult> generateAndUploadCompensationCard(
            PdfTemplateDto pdfTemplateData, Long employeeId, Long hrId) {

        log.info("Starting async generation of Compensation Card for employee: {}", employeeId);

        try {
            // Generate PDF
            MultipartFile compensationCardPdf = pdfGeneratorService.generateCompensationCardPdf(pdfTemplateData);

            // Upload to DMS
            String fileName = "Compensation_Card_" + employeeId + ".pdf";
            ResponseEntity<?> dmsResponse = callDmsToUpload(
                compensationCardPdf, employeeId, fileName, "COMPENSATION_CARD", hrId);

            // Process response
            if (dmsResponse.getStatusCode().is2xxSuccessful()) {
                @SuppressWarnings("unchecked")
                Map<String, String> responseBody = (Map<String, String>) dmsResponse.getBody();
                if (responseBody != null && responseBody.containsKey("documentUrl")) {
                    log.info("Successfully generated and uploaded Compensation Card for employee: {}", employeeId);
                    return CompletableFuture.completedFuture(new DocumentResult(
                        responseBody.get("documentUrl"),
                        responseBody.get("documentName"),
                        responseBody.get("documentType"),
                        true,
                        null
                    ));
                }
            }

            log.error("Failed to upload Compensation Card to DMS for employee: {}", employeeId);
            return CompletableFuture.completedFuture(new DocumentResult(
                null, null, "COMPENSATION_CARD", false,
                "DMS upload failed with status: " + dmsResponse.getStatusCode()
            ));

        } catch (Exception e) {
            log.error("Error generating/uploading Compensation Card for employee: {}", employeeId, e);
            return CompletableFuture.completedFuture(new DocumentResult(
                null, null, "COMPENSATION_CARD", false,
                "Exception: " + e.getMessage()
            ));
        }
    }

    /**
     * Asynchronously generates promotion letter PDF and uploads to DMS
     */
    @Async("hrDocumentTaskExecutor")
    public CompletableFuture<DocumentResult> generateAndUploadPromotionLetter(
            PdfTemplateDto pdfTemplateData, Long employeeId, Long hrId) {

        log.info("Starting async generation of Promotion Letter for employee: {}", employeeId);

        try {
            // Generate PDF
            MultipartFile promotionLetterPdf = pdfGeneratorService.generatePromotionLetterPdf(pdfTemplateData);

            // Upload to DMS
            String fileName = "Promotion_Letter_" + employeeId + ".pdf";
            ResponseEntity<?> dmsResponse = callDmsToUpload(
                promotionLetterPdf, employeeId, fileName, "PROMOTION_LETTER", hrId);

            // Process response
            if (dmsResponse.getStatusCode().is2xxSuccessful()) {
                @SuppressWarnings("unchecked")
                Map<String, String> responseBody = (Map<String, String>) dmsResponse.getBody();
                if (responseBody != null && responseBody.containsKey("documentUrl")) {
                    log.info("Successfully generated and uploaded Promotion Letter for employee: {}", employeeId);
                    return CompletableFuture.completedFuture(new DocumentResult(
                        responseBody.get("documentUrl"),
                        responseBody.get("documentName"),
                        responseBody.get("documentType"),
                        true,
                        null
                    ));
                }
            }

            log.error("Failed to upload Promotion Letter to DMS for employee: {}", employeeId);
            return CompletableFuture.completedFuture(new DocumentResult(
                null, null, "PROMOTION_LETTER", false,
                "DMS upload failed with status: " + dmsResponse.getStatusCode()
            ));

        } catch (Exception e) {
            log.error("Error generating/uploading Promotion Letter for employee: {}", employeeId, e);
            return CompletableFuture.completedFuture(new DocumentResult(
                null, null, "PROMOTION_LETTER", false,
                "Exception: " + e.getMessage()
            ));
        }
    }

    /**
     * Upload document to DMS service
     */
    private ResponseEntity<?> callDmsToUpload(MultipartFile file, Long userId, String fileName,
                                             String fileType, Long hrId) {
        if (ObjectUtils.isEmpty(file)) {
            return ResponseEntity.badRequest().build();
        }

        try {
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

            UriComponentsBuilder url = UriComponentsBuilder.fromUriString(
                webConstants.getDmsOrgDocumentUploadUrl());

            return restServices.hrRestCall(url.toUriString(), payload, headers, HttpMethod.POST, hrId);

        } catch (Exception e) {
            log.error("Exception in callDmsToUpload for file: {}", fileName, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
