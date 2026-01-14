package com.nexus.dms.service.impl;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.nexus.dms.dto.CommonFileUploadDto;
import com.nexus.dms.dto.IndividualFileUploadDto;
import com.nexus.dms.dto.OrgFileUploadDto;
import com.nexus.dms.dto.UploaderResponse;
import com.nexus.dms.entities.FolderList;
import com.nexus.dms.entities.DocumentRecord;
import com.nexus.dms.entities.DocumentType;
import com.nexus.dms.entities.UploaderType;
import com.nexus.dms.exception.ResourceNotFoundException;
import com.nexus.dms.exception.ServiceLevelException;
import com.nexus.dms.repository.FolderListRepo;
import com.nexus.dms.repository.DocumentRecordRepo;
import com.nexus.dms.service.ImplementerService;
import com.nexus.dms.service.UploaderService;
import com.nexus.dms.utils.CommonConstants;
import com.nexus.dms.utils.CommonUtils;
import com.nexus.dms.utils.RestService;
import com.nexus.dms.utils.WebConstants;

@Service
public class ImplementerServiceImpl implements ImplementerService {

    @Autowired
    private DocumentRecordRepo documentRecordRepo;

    @Autowired
    private WebConstants webConstants;

    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private RestService restService;

    @Autowired
    private UploaderService uploaderService;

    @Autowired
    private FolderListRepo folderListRepo;

    private String determineFolderPrefix(String orgType) {
        if (CommonConstants.RETAILER.equalsIgnoreCase(orgType)) {
            return CommonConstants.RETAILER_FOLDER;
        } else if (CommonConstants.SUPPLIER.equalsIgnoreCase(orgType)) {
            return CommonConstants.SUPPLIER_FOLDER;
        } else if (CommonConstants.LOGISTICS.equalsIgnoreCase(orgType)) {
            return CommonConstants.LOGISTICS_FOLDER;
        }
        return CommonConstants.COMMON_FOLDER;
    }

    private void setCommonDocumentFields(DocumentRecord documentRecord, MultipartFile file, String fileName,
            String remarks, DocumentType documentType) throws IOException {
        documentRecord.setDocumentSize(file.getSize());
        documentRecord.setMimeType(file.getContentType());
        documentRecord.setUploadedAt(new Timestamp(System.currentTimeMillis()));
        documentRecord.setDocumentName(fileName);
        documentRecord.setRemarks(remarks);
        try {
            documentRecord.setChecksum(commonUtils.generateChecksum(file.getBytes()));
        } catch (Exception e) {
            throw new IOException("Error generating checksum", e);
        }
        documentRecord.setDocumentType(documentType);
    }

    private ResponseEntity<?> handleFileUploadAndSave(DocumentRecord documentRecord, MultipartFile file,
            String fileName, String folderPrefix) throws IOException {
        try {
            ResponseEntity<UploaderResponse> fileUploaded = uploaderService.uploadFile(file, fileName, folderPrefix);

            if (fileUploaded.getStatusCode().is2xxSuccessful() && fileUploaded.getBody() != null) {
                UploaderResponse uploaderResponse = fileUploaded.getBody();
                documentRecord.setDmsId(uploaderResponse.getDmsId());
                documentRecord.setDocumentUrl(uploaderResponse.getUrl());
                documentRecord.setStatus("UPLOADED");

                FolderList folderList = folderListRepo.findByFolderName(folderPrefix)
                        .orElseThrow(() -> new ResourceNotFoundException("FolderList", "FolderName", folderPrefix));
                documentRecord.setFolderList(folderList);

                DocumentRecord savedRecord = documentRecordRepo.save(documentRecord);
                return ResponseEntity.ok(savedRecord);
            } else {
                return ResponseEntity.status(fileUploaded.getStatusCode()).body("File upload failed");
            }
        } catch (ResourceNotFoundException e) {
            throw e;
        }
    }

    @Override
    public ResponseEntity<?> individualUpload(IndividualFileUploadDto individualFileUploadDto, MultipartFile file) throws IOException {
        ResponseEntity<?> responseEntity = null;
        try {
//            MultipartFile file = individualFileUploadDto.getFile();
            commonUtils.validateFile(file);
            DocumentRecord documentRecord = new DocumentRecord();
            documentRecord.setUploaderType(UploaderType.INDIVIDUAL);
            documentRecord.setUserId(individualFileUploadDto.getUserId());

            setCommonDocumentFields(documentRecord, file, individualFileUploadDto.getFileName(),
                    individualFileUploadDto.getRemarks(), individualFileUploadDto.getDocumentType());

            String orgType = fetchOrgTypeForUser(individualFileUploadDto.getUserId(), documentRecord);
            String folderPrefix = determineFolderPrefix(orgType);

            documentRecord
                    .setStorageLocation(webConstants.getBucketUrl() + CommonConstants.MAIN_BUCKET + "/" + folderPrefix);
            responseEntity = handleFileUploadAndSave(documentRecord, file, individualFileUploadDto.getFileName(),
                    folderPrefix);

        } catch (Exception e) {
            throw new ServiceLevelException("ImplementerService", e.getMessage(), "individualUpload",
                    e.getClass().getSimpleName(), "Error occurred while uploading individual file");
        }
        return responseEntity;
    }

    private String fetchOrgTypeForUser(Long userId, DocumentRecord documentRecord) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(webConstants.getFetchUserOrgDetailsUrl());
        uriBuilder.queryParam("userId", userId);

        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(CommonConstants.AUTHORIZATION, commonUtils.getToken());
        headers.put(CommonConstants.CONTENT_TYPE, CommonConstants.APPLICATION_JSON);

        ResponseEntity<?> response = restService.dmsRestCall(uriBuilder.toUriString(), null, headers,
                HttpMethod.GET, userId);

        String orgType = null;
        if (response.getStatusCode().is2xxSuccessful()) {
            Object responseBody = response.getBody();
            if (responseBody instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = (Map<String, Object>) responseBody;
                if (responseMap.containsKey("orgId")) {
                    documentRecord.setOrgId(Long.valueOf(responseMap.get("orgId").toString()));
                }
                if (responseMap.containsKey("orgType")) {
                    orgType = responseMap.get("orgType").toString();
                }
            }
        } else {
            documentRecord.setOrgId(null);
        }
        return orgType;
    }

    @Override
    public ResponseEntity<?> orgUpload(OrgFileUploadDto orgFileUploadDto) throws IOException {
        ResponseEntity<?> responseEntity = null;
        try {
            MultipartFile file = orgFileUploadDto.getFile();
            commonUtils.validateFile(file);
            DocumentRecord documentRecord = new DocumentRecord();
            documentRecord.setOrgId(orgFileUploadDto.getOrgId());
            documentRecord.setUploaderType(UploaderType.ORGANIZATION);

            setCommonDocumentFields(documentRecord, file, orgFileUploadDto.getFileName(),
                    orgFileUploadDto.getRemarks(), orgFileUploadDto.getDocumentType());

            String folderPrefix = determineFolderPrefix(
                    !ObjectUtils.isEmpty(orgFileUploadDto.getOrgType()) ? orgFileUploadDto.getOrgType().name()
                            : null);

            documentRecord
                    .setStorageLocation(webConstants.getBucketUrl() + CommonConstants.MAIN_BUCKET + "/" + folderPrefix);
            responseEntity = handleFileUploadAndSave(documentRecord, file, orgFileUploadDto.getFileName(),
                    folderPrefix);

        } catch (Exception e) {
            throw new ServiceLevelException("ImplementerService", e.getMessage(), "orgUpload",
                    e.getClass().getSimpleName(), "Error occurred while uploading organizational file");
        }
        return responseEntity;
    }

    @Override
    public ResponseEntity<?> commonUpload(CommonFileUploadDto commonFileUploadDto) throws IOException {
        ResponseEntity<?> responseEntity = null;
        try {
            MultipartFile file = commonFileUploadDto.getFile();
            commonUtils.validateFile(file);
            DocumentRecord documentRecord = new DocumentRecord();
            documentRecord.setUserId(commonFileUploadDto.getUserId());
            documentRecord.setOrgId(commonFileUploadDto.getOrgId());
            documentRecord.setUploaderType(UploaderType.COMMON);

            setCommonDocumentFields(documentRecord, file, commonFileUploadDto.getFileName(),
                    commonFileUploadDto.getRemarks(), commonFileUploadDto.getDocumentType());

            String folderPrefix = determineFolderPrefix(
                    !ObjectUtils.isEmpty(commonFileUploadDto.getOrgType()) ? commonFileUploadDto.getOrgType().name()
                            : null);

            documentRecord
                    .setStorageLocation(webConstants.getBucketUrl() + CommonConstants.MAIN_BUCKET + "/" + folderPrefix);
            responseEntity = handleFileUploadAndSave(documentRecord, file, commonFileUploadDto.getFileName(),
                    folderPrefix);

        } catch (Exception e) {
            throw new ServiceLevelException("ImplementerService", e.getMessage(), "commonUpload",
                    e.getClass().getSimpleName(), "Error occurred while uploading common file");
        }
        return responseEntity;
    }

}
