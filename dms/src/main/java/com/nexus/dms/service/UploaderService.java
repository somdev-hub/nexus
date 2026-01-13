package com.nexus.dms.service;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.nexus.dms.dto.UploaderResponse;

public interface UploaderService {

    ResponseEntity<UploaderResponse> uploadFile(MultipartFile file, String fileName, String folderPrefix)
            throws IOException;

    Boolean deleteFile(String dmsId, String bucketName);
}
