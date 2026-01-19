package com.nexus.hr.service.implementations;

import com.nexus.hr.entity.HrDocument;
import com.nexus.hr.entity.HrEntity;
import com.nexus.hr.entity.Position;
import com.nexus.hr.exception.ServiceLevelException;
import com.nexus.hr.payload.GeneratedPdfDto;
import com.nexus.hr.payload.HrInitRequestDto;
import com.nexus.hr.payload.PdfTemplateDto;
import com.nexus.hr.repository.HrEntityRepo;
import com.nexus.hr.service.interfaces.HrService;
import com.nexus.hr.utils.CommonConstants;
import com.nexus.hr.utils.CommonUtils;
import com.nexus.hr.utils.RestServices;
import com.nexus.hr.utils.WebConstants;
import com.nexus.hr.views.PdfGeneratorService;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HrServiceImpl implements HrService {

    private final HrEntityRepo hrEntityRepo;

    private final ModelMapper modelMapper;

    private final PdfGeneratorService pdfGeneratorService;

    private final CommonUtils commonUtils;

    private final WebConstants webConstants;

    private final RestServices restServices;

    public HrServiceImpl(HrEntityRepo hrEntityRepo, ModelMapper modelMapper, PdfGeneratorService pdfGeneratorService, CommonUtils commonUtils, WebConstants webConstants, RestServices restServices) {
        this.hrEntityRepo = hrEntityRepo;
        this.modelMapper = modelMapper;
        this.pdfGeneratorService = pdfGeneratorService;
        this.commonUtils = commonUtils;
        this.webConstants = webConstants;
        this.restServices = restServices;
    }

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

            // Save HR entity
            HrEntity savedHrEntity = hrEntityRepo.save(hrEntity);

            // Create response with PDF data
            GeneratedPdfDto pdfResponse = GeneratedPdfDto.builder()
                    .employeeId(savedHrEntity.getEmployeeId())
                    .joiningLetterPdf(joiningLetterPdf)
                    .letterOfIntentPdf(letterOfIntentPdf)
                    .joiningLetterFileName("Joining_Letter_" + savedHrEntity.getEmployeeId() + ".pdf")
                    .letterOfIntentFileName("Letter_Of_Intent_" + savedHrEntity.getEmployeeId() + ".pdf")
                    .build();

            response = ResponseEntity.ok(pdfResponse);

        } catch (Exception e) {
            throw new ServiceLevelException(
                    "HR Service",
                    "Exception occurred while initializing HR module",
                    "initHr",
                    e.getClass().getName(),
                    e.getMessage()
            );
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

                response = restServices.hrRestCall(
                        url.toUriString(),
                        payload,
                        headers,
                        CommonConstants.POST,
                        hrId
                );

            } else {
                response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "HR Service",
                    "Exception occurred while uploading document to DMS",
                    "callDmsToUpload",
                    e.getClass().getName(),
                    e.getMessage()
            );
        }

        return response;
    }

    /**
     * Build PDF template data from HR initialization request
     */
    private PdfTemplateDto buildPdfTemplateData(HrInitRequestDto hrInitRequestDto, Timestamp effectiveFrom) {
        return PdfTemplateDto.builder()
                .employeeId(hrInitRequestDto.getEmployeeId())
                .employeeName("Employee") // You may need to fetch this from employee service
                .department(hrInitRequestDto.getDepartment())
                .position(hrInitRequestDto.getTitle())
                .remarks(hrInitRequestDto.getRemarks())
                .effectiveFrom(effectiveFrom)
                .organizationName("Organization") // Configure from properties
                .organizationAddress("Organization Address") // Configure from properties
                .hrContactEmail("hr@organization.com") // Configure from properties
                .hrContactPhone("+1-XXX-XXX-XXXX") // Configure from properties
                .build();
    }
}
