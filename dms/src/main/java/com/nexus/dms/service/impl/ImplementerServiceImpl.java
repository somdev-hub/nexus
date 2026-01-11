package com.nexus.dms.service.impl;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.nexus.dms.dto.IndividualFileUploadDto;
import com.nexus.dms.dto.OrgFileUploadDto;
import com.nexus.dms.repository.DocumentRecordRepo;
import com.nexus.dms.service.ImplementerService;

@Service
public class ImplementerServiceImpl implements ImplementerService {

    @Autowired
    private DocumentRecordRepo documentRecordRepo;

    @Override
    public ResponseEntity<?> individualUpload(IndividualFileUploadDto individualFileUploadDto) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'individualUpload'");
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
