package com.nexus.hr.service.interfaces;

import com.nexus.hr.payload.BulkRegularizationRequestDto;
import org.springframework.http.ResponseEntity;

import java.sql.Date;
import java.util.List;
import java.util.Map;

public interface TimeManagementService {

    ResponseEntity<?> toggleAttendance(Long hrId);

    ResponseEntity<?> bulkRegularize(List<BulkRegularizationRequestDto> bulkRegularizationRequestDtos);

    ResponseEntity<?> applyWeeklyOff(Long hrId, Date fromDate, Date toDate, String remarks);

    ResponseEntity<?> applyHoliday(Long hrId, Date fromDate, Date toDate, String remarks);

    ResponseEntity<?> getEmployeesAttendance(Map<String, Object> requestBody);

    ResponseEntity<?> toggleAttandenceByEmpId(Long empId);
}
