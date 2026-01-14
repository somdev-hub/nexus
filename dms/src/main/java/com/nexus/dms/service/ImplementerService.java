package com.nexus.dms.service;

import java.io.IOException;

import org.springframework.http.ResponseEntity;

import com.nexus.dms.dto.CommonFileUploadDto;
import com.nexus.dms.dto.IndividualFileUploadDto;
import com.nexus.dms.dto.OrgFileUploadDto;
import org.springframework.web.multipart.MultipartFile;

public interface ImplementerService {

    ResponseEntity<?> individualUpload(IndividualFileUploadDto individualFileUploadDto, MultipartFile file) throws IOException;

    ResponseEntity<?> orgUpload(OrgFileUploadDto orgFileUploadDto) throws IOException;

    ResponseEntity<?> commonUpload(CommonFileUploadDto commonFileUploadDto) throws IOException;
}
