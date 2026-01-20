package com.nexus.hr.service.implementations;

import com.nexus.hr.entity.*;
import com.nexus.hr.exception.ServiceLevelException;
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
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
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

            // Save HR entity
            HrEntity savedHrEntity = hrEntityRepo.save(hrEntity);

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


            Compensation compensation = modelMapper.map(hrInitRequestDto.getCompensation(), Compensation.class);
            compensation.setBonuses(hrInitRequestDto.getCompensation().getBonuses().stream().map(bonus -> modelMapper.map(bonus, Bonus.class)).toList());
            compensation.setDeductions(hrInitRequestDto.getCompensation().getDeductions().stream().map(deductionDto -> modelMapper.map(deductionDto, Deduction.class)).toList());
            if (compensationCardDmsResponse.getStatusCode().is2xxSuccessful()) {
                @SuppressWarnings("unchecked") Map<String, String> responseBody = (Map<String, String>) compensationCardDmsResponse.getBody();
                assert responseBody != null;
                if (responseBody.containsKey("documentUrl")) {
                    HrDocument hrDocument = new HrDocument();
                    hrDocument.setDocumentUrl(responseBody.get("documentUrl"));
                    hrDocument.setDocumentName(responseBody.get("documentName"));
                    hrDocument.setHrDocumentType(responseBody.get("documentType"));
                    hrDocument.setCompensation(compensation);
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

            compensation.setHrEntity(hrEntity);
            hrEntity.setCompensation(compensation);

            // send communication
            EmailCommunicationDto emailCommunicationDto = new EmailCommunicationDto();
            emailCommunicationDto.setSenderEmail("hr@nexus.com");
            emailCommunicationDto.setRecipientEmails(List.of(hrInitRequestDto.getPersonalEmail()));
            emailCommunicationDto.setSubject("Update on your application");
            emailCommunicationDto.setBody(CommonConstants.HR_INIT_EMAIL_TEMPLATE.formatted(hrInitRequestDto.getFullName(), hrEntity.getEmployeeId()));
            emailCommunicationDto.setAttachments(List.of(new EmailAttachmentDto("Joining Letter", "PDF", joiningLetterUrl),
                    new EmailAttachmentDto("Letter of Intent", "PDF", letterOfIntentUrl),
                    new EmailAttachmentDto("Compensation Card", "PDF", compensationCardUrl)));

            communicationService.sendCommunicationOverEmail(emailCommunicationDto);

            response = ResponseEntity.ok(HrInitResponse.builder().hrId(hrEntity.getHrId()).joiningLetterUrl(joiningLetterUrl).letterOfIntentUrl(letterOfIntentUrl).compensationCardUrl(compensationCardUrl).build());

        } catch (Exception e) {
            throw new ServiceLevelException("HR Service", "Exception occurred while initializing HR module", "initHr", e.getClass().getName(), e.getMessage());
        }

        return response;
    }

    private ResponseEntity<?> callDmsToUpload(MultipartFile file, Long userId, String fileName, String fileType, Long hrId) {
        ResponseEntity<?> response = null;
        try {
            if (!ObjectUtils.isEmpty(file)) {
                Map<String, Object> payload = new ConcurrentHashMap<>();
                Map<String, Object> dto = new HashMap<>();
                dto.put("userId", userId);
                dto.put("fileName", fileName);
                dto.put("remarks", "Profile Photo Upload");
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
            throw new ServiceLevelException("HR Service", "Exception occurred while uploading document to DMS", "callDmsToUpload", e.getClass().getName(), e.getMessage());
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
