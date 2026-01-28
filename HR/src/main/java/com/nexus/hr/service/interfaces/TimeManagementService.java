package com.nexus.hr.service.interfaces;

import com.nexus.hr.payload.BulkRegularizationRequestDto;
import org.springframework.http.ResponseEntity;

import java.sql.Date;
import java.util.List;

public interface TimeManagementService {

    ResponseEntity<?> toggleAttendance(Long hrId);

    ResponseEntity<?> bulkRegularize(List<BulkRegularizationRequestDto> bulkRegularizationRequestDtos);

    ResponseEntity<?> applyWeeklyOff(Long hrId, Date fromDate, Date toDate, String remarks);

    ResponseEntity<?> applyHoliday(Long hrId, Date fromDate, Date toDate, String remarks);
}
