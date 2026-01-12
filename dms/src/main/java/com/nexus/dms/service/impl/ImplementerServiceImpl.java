package com.nexus.dms.service.impl;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.nexus.dms.dto.IndividualFileUploadDto;
import com.nexus.dms.dto.OrgFileUploadDto;
import com.nexus.dms.dto.UploaderResponse;
import com.nexus.dms.entities.BucketList;
import com.nexus.dms.entities.DocumentRecord;
import com.nexus.dms.exception.ResourceNotFoundException;
import com.nexus.dms.exception.ServiceLevelException;
import com.nexus.dms.repository.BucketListRepo;
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
    private BucketListRepo bucketListRepo;

    @Override
    public ResponseEntity<?> individualUpload(IndividualFileUploadDto individualFileUploadDto) throws IOException {
        ResponseEntity<?> responseEntity = null;
        try {
            MultipartFile file = individualFileUploadDto.getFile();
            commonUtils.validateFile(file);
            DocumentRecord documentRecord = new DocumentRecord();
            documentRecord.setDocumentSize(file.getSize());
            documentRecord.setMimeType(file.getContentType());
            documentRecord.setUploadedAt(new Timestamp(System.currentTimeMillis()));
            documentRecord.setDocumentName(individualFileUploadDto.getFileName());
            documentRecord.setUserId(individualFileUploadDto.getUserId());
            documentRecord.setRemarks(individualFileUploadDto.getRemarks());
            documentRecord.setChecksum(commonUtils.generateChecksum(file.getBytes()));

            UriComponentsBuilder uriBuilder = UriComponentsBuilder
                    .fromUriString(webConstants.getFetchUserOrgDetailsUrl());
            uriBuilder.queryParam("userId", individualFileUploadDto.getUserId());

            Map<String, String> headers = new ConcurrentHashMap<>();
            headers.put(CommonConstants.AUTHORIZATION, commonUtils.getToken());
            headers.put(CommonConstants.CONTENT_TYPE, CommonConstants.APPLICATION_JSON);

            ResponseEntity<?> response = restService.dmsRestCall(uriBuilder.toUriString(), null, headers,
                    HttpMethod.GET, individualFileUploadDto.getUserId());

            String orgType = null, bucket = null;
            if (response.getStatusCode().is2xxSuccessful()) {
                Object responseBody = response.getBody();
                if (responseBody instanceof Map) {
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

            if (CommonConstants.RETAILER.equalsIgnoreCase(orgType)) {
                bucket = CommonConstants.RETAILER_BUCKET;
            } else if (CommonConstants.SUPPLIER.equalsIgnoreCase(orgType)) {
                bucket = CommonConstants.SUPPLIER_BUCKET;
            } else if (CommonConstants.LOGISTICS.equalsIgnoreCase(orgType)) {
                bucket = CommonConstants.LOGISTICS_BUCKET;
            } else {
                bucket = CommonConstants.COMMON_BUCKET;
            }

            ResponseEntity<UploaderResponse> fileUploaded = uploaderService.uploadFile(file,
                    individualFileUploadDto.getFileName(), bucket);

            if (fileUploaded.getStatusCode().is2xxSuccessful() && fileUploaded.getBody() != null) {
                UploaderResponse uploaderResponse = fileUploaded.getBody();
                documentRecord.setDmsId(uploaderResponse.getDmsId());
                documentRecord.setDocumentUrl(uploaderResponse.getUrl());

                final String BUCKET = bucket;
                BucketList bucketList = bucketListRepo.findByBucketName(bucket)
                        .orElseThrow(() -> new ResourceNotFoundException("BucketList", "BucketName", BUCKET));
                documentRecord.setBucketList(bucketList);

                DocumentRecord savedRecord = documentRecordRepo.save(documentRecord);
                responseEntity = ResponseEntity.ok(savedRecord);

            } else {
                responseEntity = ResponseEntity.status(fileUploaded.getStatusCode()).body("File upload failed");
            }

        } catch (Exception e) {
            throw new ServiceLevelException(
                    "ImplementerService",
                    e.getMessage(),
                    "individualUpload",
                    e.getClass().getSimpleName(),
                    "Error occurred while uploading individual file");
        }

        return responseEntity;
    }

    @Override
    public ResponseEntity<?> orgUpload(OrgFileUploadDto orgFileUploadDto) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'orgUpload'");
    }

    @Override
    public ResponseEntity<?> commonUpload() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'commonUpload'");
    }

}
