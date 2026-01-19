package com.nexus.hr.service.interfaces;

import com.nexus.hr.payload.HrInitRequestDto;
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
    public ResponseEntity<?> initHr(HrInitRequestDto hrInitRequestDto);
}
