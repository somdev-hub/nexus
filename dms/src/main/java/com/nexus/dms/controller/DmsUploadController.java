package com.nexus.dms.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nexus.dms.annotation.LogActivity;
import com.nexus.dms.dto.CommonFileUploadDto;
import com.nexus.dms.dto.IndividualFileUploadDto;
import com.nexus.dms.dto.OrgFileUploadDto;
import com.nexus.dms.exception.UnauthorizedException;
import com.nexus.dms.service.ImplementerService;
import com.nexus.dms.utils.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/dms/upload")
public class DmsUploadController {

    @Autowired
    private ImplementerService implementerService;

    @Autowired
    private CommonUtils commonUtils;

    /**
     * Upload individual file
     * Validates authorization and request body
     * Delegates to service layer
     * AOP handles logging of all requests/responses/exceptions
     */
    @LogActivity("Individual File Upload")
    @PostMapping(value = "/individual", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> individualUpload(@RequestPart(name = "dto") IndividualFileUploadDto dto,
                                              @RequestPart("file") MultipartFile file,
                                              @RequestHeader("Authorization") String authHeader) throws JsonProcessingException, IOException {
        if (ObjectUtils.isEmpty(authHeader) || !commonUtils.validateToken(authHeader)) {
            throw new UnauthorizedException("Unauthorized! Please use credentials", "Unable to validate token");
        }

        if (ObjectUtils.isEmpty(dto)) {
            throw new IllegalArgumentException("Request body is missing");
        }

        return implementerService.individualUpload(dto, file);
    }

    /**
     * Upload organization file
     * Validates authorization and request body
     * Delegates to service layer
     * AOP handles logging of all requests/responses/exceptions
     */
    @LogActivity("Organization File Upload")
    @PostMapping(value = "/org", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> orgUpload(@RequestPart(name = "dto") OrgFileUploadDto dto,
                                       @RequestPart("file") MultipartFile file,
                                       @RequestHeader("Authorization") String authHeader) throws JsonProcessingException, IOException {
        if (ObjectUtils.isEmpty(authHeader) || !commonUtils.validateToken(authHeader)) {
            throw new UnauthorizedException("Unauthorized! Please use credentials", "Unable to validate token");
        }

        if (ObjectUtils.isEmpty(dto)) {
            throw new IllegalArgumentException("Request body is missing");
        }

        return implementerService.orgUpload(dto, file);
    }

    /**
     * Upload common file
     * Validates authorization and request body
     * Delegates to service layer
     * AOP handles logging of all requests/responses/exceptions
     */
    @LogActivity("Common File Upload")
    @PostMapping(value = "/common", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> commonUpload(@RequestPart(name = "dto") CommonFileUploadDto dto,
                                          @RequestPart("file") MultipartFile file,
                                          @RequestHeader("Authorization") String authHeader) throws JsonProcessingException, IOException {
        if (ObjectUtils.isEmpty(authHeader) || !commonUtils.validateToken(authHeader)) {
            throw new UnauthorizedException("Unauthorized! Please use credentials", "Unable to validate token");
        }

        if (ObjectUtils.isEmpty(dto)) {
            throw new IllegalArgumentException("Request body is missing");
        }

        return implementerService.commonUpload(dto, file);
    }

}
