package com.nexus.hr.service.interfaces;

import com.nexus.hr.model.entities.Position;
import com.nexus.hr.model.enums.HrRequestStatus;
import com.nexus.hr.payload.CompensationDto;
import com.nexus.hr.payload.HrInitRequestDto;
import com.nexus.hr.payload.HrRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public interface HrService {

    /**
     * Initialize HR for an employee with automatic PDF generation
     * This method will:
     * 1. Create HR entity with position and documents
     * 2. Generate Joining Letter PDF
     * 3. Generate Letter of Intent PDF
     * 4. Return both PDFs along with file names
     *
     * @param hrInitRequestDto HR initialization request containing employee details
     * @return ResponseEntity containing GeneratedPdfDto with PDF files and metadata
     */
    ResponseEntity<?> initHr(HrInitRequestDto hrInitRequestDto);

    ResponseEntity<?> takeActionForHrRequests(Long requestId, HrRequestStatus action, String resolutionRemarks);

    ResponseEntity<Page<HrRequestDto>> getAllHrRequests(Pageable pageable);

    ResponseEntity<?> promoteEmployee(Long hrId, Position position, CompensationDto compensation);

    ResponseEntity<?> rewardAppraisal(Long hrId, CompensationDto compensation);


}
