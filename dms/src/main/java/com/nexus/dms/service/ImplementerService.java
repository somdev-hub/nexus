package com.nexus.dms.service;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import com.nexus.dms.dto.IndividualFileUploadDto;
import com.nexus.dms.dto.OrgFileUploadDto;

public interface ImplementerService {

    ResponseEntity<?> individualUpload(IndividualFileUploadDto individualFileUploadDto) throws IOException;

    ResponseEntity<?> orgUpload(OrgFileUploadDto orgFileUploadDto) throws IOException;

    ResponseEntity<?> commonUpload();
}
