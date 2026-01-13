package com.nexus.dms.controller;

import java.sql.Timestamp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nexus.dms.dto.ErrorResponseDto;
import com.nexus.dms.exception.UnauthorizedException;
import com.nexus.dms.service.FolderListService;
import com.nexus.dms.utils.CommonUtils;
import com.nexus.dms.utils.Logger;

@RestController
@RequestMapping("/folder-list")
public class FolderListController {

    @Autowired
    private FolderListService folderListService;

    @Autowired
    private Logger logger;

    @Autowired
    private CommonUtils commonUtils;

    @GetMapping("/set-default-folders")
    public ResponseEntity<?> setFolders(@RequestHeader("Authorization") String authHeader)
            throws JsonProcessingException {
        if (ObjectUtils.isEmpty(authHeader) || !commonUtils.validateToken(authHeader)) {
            throw new UnauthorizedException("Unauthorized! Please use credentials", "Unable to validate token");
        }
        ResponseEntity<?> response = null;
        try {
            folderListService.setFolderLists();
            response = ResponseEntity.ok("Default folders persisted successfully");
        } catch (Exception e) {
            ErrorResponseDto errorResponse = new ErrorResponseDto(
                    "Internal Server Error",
                    500,
                    new Timestamp(System.currentTimeMillis()),
                    "An error occurred during setting folders",
                    e.getMessage());
            response = ResponseEntity.status(500).body(errorResponse);
        } finally {
            HttpStatus status = response != null ? HttpStatus.valueOf(response.getStatusCode().value()) : null;
            logger.saveLogs("/folder-list/set-default-folders", HttpMethod.GET, status, null,
                    response != null ? response.getBody() : null, 0L);
        }
        return response;
    }

}
