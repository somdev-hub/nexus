package com.nexus.hr.service.interfaces;

import com.nexus.hr.payload.BulkRegularizationRequestDto;
import org.springframework.http.ResponseEntity;

public interface TimeManagementService {

    ResponseEntity<?> toggleAttendance(Long hrId);

    ResponseEntity<?> bulkRegularize(BulkRegularizationRequestDto bulkRegularizationRequestDto);
}
