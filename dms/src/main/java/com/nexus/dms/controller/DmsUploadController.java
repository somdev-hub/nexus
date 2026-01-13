package com.nexus.dms.controller;

import java.sql.Timestamp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nexus.dms.dto.CommonFileUploadDto;
import com.nexus.dms.dto.ErrorResponseDto;
import com.nexus.dms.dto.IndividualFileUploadDto;
import com.nexus.dms.dto.OrgFileUploadDto;
import com.nexus.dms.entities.DocumentRecord;
import com.nexus.dms.exception.UnauthorizedException;
import com.nexus.dms.service.ImplementerService;
import com.nexus.dms.utils.CommonUtils;
import com.nexus.dms.utils.Logger;

@RestController
@RequestMapping("/dms/upload")
public class DmsUploadController {

    @Autowired
    private ImplementerService implementerService;

    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private Logger logger;

    @PostMapping("/individual")
    public ResponseEntity<?> individualUpload(@RequestBody IndividualFileUploadDto dto,
            @RequestHeader("Authorization") String authHeader) throws JsonProcessingException {
        if (ObjectUtils.isEmpty(authHeader) || !commonUtils.validateToken(authHeader)) {
            throw new UnauthorizedException("Unauthorized! Please use credentials", "Unable to validate token");
        }

        if (ObjectUtils.isEmpty(dto)) {
            ErrorResponseDto errorResponse = new ErrorResponseDto(
                    "Bad Request",
                    400,
                    new Timestamp(System.currentTimeMillis()),
                    "Request body is missing",
                    "The request body cannot be null or empty");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        ResponseEntity<?> response = null;
        try {
            response = implementerService.individualUpload(dto);
        } catch (Exception e) {
            ErrorResponseDto errorResponse = new ErrorResponseDto(
                    "Internal Server Error",
                    500,
                    new Timestamp(System.currentTimeMillis()),
                    "An error occurred during file upload",
                    e.getMessage());
            response = ResponseEntity.status(500).body(errorResponse);
        } finally {
            Long documentRecordId = null;
            if (response != null && response.getBody() instanceof DocumentRecord) {
                documentRecordId = ((DocumentRecord) response.getBody()).getId();
            }
            HttpStatus status = response != null ? HttpStatus.valueOf(response.getStatusCode().value()) : null;
            logger.saveLogs("/dms/upload/individual", HttpMethod.POST, status, dto,
                    response != null ? response.getBody() : null, documentRecordId);
        }

        return response;
    }

    @PostMapping("/org")
    public ResponseEntity<?> orgUpload(@RequestBody OrgFileUploadDto dto,
            @RequestHeader("Authorization") String authHeader) throws JsonProcessingException {
        if (ObjectUtils.isEmpty(authHeader) || !commonUtils.validateToken(authHeader)) {
            throw new UnauthorizedException("Unauthorized! Please use credentials", "Unable to validate token");
        }

        if (ObjectUtils.isEmpty(dto)) {
            ErrorResponseDto errorResponse = new ErrorResponseDto(
                    "Bad Request",
                    400,
                    new Timestamp(System.currentTimeMillis()),
                    "Request body is missing",
                    "The request body cannot be null or empty");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        ResponseEntity<?> response = null;
        try {
            response = implementerService.orgUpload(dto);
        } catch (Exception e) {
            ErrorResponseDto errorResponse = new ErrorResponseDto(
                    "Internal Server Error",
                    500,
                    new Timestamp(System.currentTimeMillis()),
                    "An error occurred during file upload",
                    e.getMessage());
            response = ResponseEntity.status(500).body(errorResponse);
        } finally {
            Long documentRecordId = 0L;
            if (response != null && response.getBody() instanceof DocumentRecord) {
                documentRecordId = ((DocumentRecord) response.getBody()).getId();
            }
            HttpStatus status = response != null ? HttpStatus.valueOf(response.getStatusCode().value()) : null;
            logger.saveLogs("/dms/upload/org", HttpMethod.POST, status, dto,
                    response != null ? response.getBody() : null, documentRecordId);
        }

        return response;
    }

    @PostMapping("/common")
    public ResponseEntity<?> commonUpload(@RequestBody CommonFileUploadDto dto,
            @RequestHeader("Authorization") String authHeader) throws JsonProcessingException {
        if (ObjectUtils.isEmpty(authHeader) || !commonUtils.validateToken(authHeader)) {
            throw new UnauthorizedException("Unauthorized! Please use credentials", "Unable to validate token");
        }
        if (ObjectUtils.isEmpty(dto)) {
            ErrorResponseDto errorResponse = new ErrorResponseDto(
                    "Bad Request",
                    400,
                    new Timestamp(System.currentTimeMillis()),
                    "Request body is missing",
                    "The request body cannot be null or empty");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        ResponseEntity<?> response = null;

        try {
            response = implementerService.commonUpload(dto);
        } catch (Exception e) {
            ErrorResponseDto errorResponse = new ErrorResponseDto(
                    "Internal Server Error",
                    500,
                    new Timestamp(System.currentTimeMillis()),
                    "An error occurred during file upload",
                    e.getMessage());
            response = ResponseEntity.status(500).body(errorResponse);
        } finally {
            Long documentRecordId = 0L;
            if (response != null && response.getBody() instanceof DocumentRecord) {
                documentRecordId = ((DocumentRecord) response.getBody()).getId();
            }
            HttpStatus status = response != null ? HttpStatus.valueOf(response.getStatusCode().value()) : null;
            logger.saveLogs("/dms/upload/common", HttpMethod.POST, status, dto,
                    response != null ? response.getBody() : null, documentRecordId);
        }
        return response;
    }

}
