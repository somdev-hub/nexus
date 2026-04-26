package com.nexus.hr.service.implementations;

import com.nexus.hr.exception.ResourceNotFoundException;
import com.nexus.hr.exception.ServiceLevelException;
import com.nexus.hr.model.entities.Compensation;
import com.nexus.hr.model.entities.HrEntity;
import com.nexus.hr.model.entities.HrRequest;
import com.nexus.hr.model.entities.TimeManagement;
import com.nexus.hr.model.enums.HrRequestStatus;
import com.nexus.hr.model.enums.HrRequestType;
import com.nexus.hr.payload.BulkRegularizationRequestDto;
import com.nexus.hr.payload.response.AttendanceResponse;
import com.nexus.hr.payload.response.TimeManagementQuickResponseDto;
import com.nexus.hr.repository.HrEntityRepo;
import com.nexus.hr.repository.HrRequestRepo;
import com.nexus.hr.repository.TimeManagementRepo;
import com.nexus.hr.service.interfaces.TimeManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class TimeManagementServiceImpl implements TimeManagementService {

    private static final long WORK_HOURS_THRESHOLD = 8; // 8 hours in milliseconds = 28800000
    private final TimeManagementRepo timeManagementRepo;
    private final HrEntityRepo hrEntityRepo;
    private final HrRequestRepo hrRequestRepo;

    @Override
    public ResponseEntity<?> toggleAttendance(Long hrId) {
        try {
            // Validate HrEntity exists
            HrEntity hrEntity = hrEntityRepo.findById(hrId)
                    .orElseThrow(() -> new ResourceNotFoundException("HrEntity", "hrId", hrId));

            // Check if employee is active
            if (hrEntity.getIsActive() == null || !hrEntity.getIsActive()) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Employee is not active. Attendance cannot be logged.");
                response.put("hrId", hrId);
                response.put("isActive", hrEntity.getIsActive());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            LocalDate today = LocalDate.now();
            TimeManagement todayRecord = timeManagementRepo.findByDayMonthYearAndHrEntity(
                    today.getDayOfMonth(),
                    today.getMonthValue(),
                    today.getYear(),
                    hrId);

            // If no record exists for today, create a new one
            if (todayRecord == null) {
                return handleNewDayEntry(hrEntity, today);
            } else {
                return handleExistingDayEntry(todayRecord, hrEntity, today);
            }

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "TimeManagementService",
                    "Error while toggling attendance",
                    "toggleAttendance",
                    e.getClass().getName(),
                    e.getMessage());
        }
    }

    /**
     * Handle new entry for today - check previous day and close it if needed
     */
    private ResponseEntity<?> handleNewDayEntry(HrEntity hrEntity, LocalDate today) {
        TimeManagement newRecord = new TimeManagement();
        newRecord.setHrEntity(hrEntity);
        newRecord.setDay(today.getDayOfMonth());
        newRecord.setMonth(today.getMonthValue());
        newRecord.setYear(today.getYear());
        newRecord.setCreatedOn(Timestamp.valueOf(LocalDateTime.now()));

        // Set check-in time
        LocalDateTime now = LocalDateTime.now();
        newRecord.setCheckInTime(Timestamp.valueOf(now));
        newRecord.setIsPresent(true);
        newRecord.setIsOnLeave(false);

        // Handle previous day record - close it if not closed
        handlePreviousDayClose(hrEntity, today);

        // Save and return
        TimeManagement savedRecord = timeManagementRepo.save(newRecord);
        return buildAttendanceResponse(savedRecord, "Check-in recorded successfully");
    }

    /**
     * Handle existing day entry - manage break and checkout times
     */
    private ResponseEntity<?> handleExistingDayEntry(TimeManagement todayRecord, HrEntity hrEntity, LocalDate today) {
        LocalDateTime now = LocalDateTime.now();
        Timestamp currentTime = Timestamp.valueOf(now);

        if (Boolean.TRUE.equals(todayRecord.getIsOnLeave())) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Attendance cannot be toggled for a leave/holiday record.");
            response.put("timeManagementId", todayRecord.getTimeManagementId());
            response.put("isOnLeave", true);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        if (todayRecord.getCheckInTime() == null) {
            todayRecord.setCheckInTime(currentTime);
            todayRecord.setIsPresent(true);
            todayRecord.setIsOnLeave(false);
            timeManagementRepo.save(todayRecord);
            return buildAttendanceResponse(todayRecord, "Check-in recorded successfully");
        }

        // 2nd call: if today's record only has check-in, mark checkout and break start together.
        if (todayRecord.getCheckOutTime() == null && todayRecord.getBreakStartTime() == null) {
            todayRecord.setCheckOutTime(currentTime);
            todayRecord.setBreakStartTime(currentTime);
            calculateWorkingHours(todayRecord);
            timeManagementRepo.save(todayRecord);
            return buildAttendanceResponse(todayRecord, "Checkout and break start recorded");
        }

        // 3rd call: break end is still missing.
        if (todayRecord.getBreakEndTime() == null) {
            todayRecord.setBreakEndTime(currentTime);
            calculateWorkingHours(todayRecord);
            timeManagementRepo.save(todayRecord);
            return buildAttendanceResponse(todayRecord, "Break end recorded");
        }

        // 4th/5th/... calls: alternate between checkout and break end based on their
        // current ordering.
        if (todayRecord.getCheckOutTime() == null) {
            todayRecord.setCheckOutTime(currentTime);
            calculateWorkingHours(todayRecord);
            timeManagementRepo.save(todayRecord);
            return buildAttendanceResponse(todayRecord, "Checkout recorded");
        }

        if (todayRecord.getBreakEndTime().after(todayRecord.getCheckOutTime())) {
            todayRecord.setCheckOutTime(currentTime);
            calculateWorkingHours(todayRecord);
            timeManagementRepo.save(todayRecord);
            return buildAttendanceResponse(todayRecord, "Checkout updated");
        }

        if (todayRecord.getBreakEndTime().before(todayRecord.getCheckOutTime())) {
            todayRecord.setBreakEndTime(currentTime);
            calculateWorkingHours(todayRecord);
            timeManagementRepo.save(todayRecord);
            return buildAttendanceResponse(todayRecord, "Break end updated");
        }

        // Equal timestamps are a rare edge case; default to updating checkout so the
        // record continues to move forward deterministically.
        todayRecord.setCheckOutTime(currentTime);
        calculateWorkingHours(todayRecord);
        timeManagementRepo.save(todayRecord);
        return buildAttendanceResponse(todayRecord, "Checkout updated");
    }

    /**
     * Handle closing previous day's record if checkout is missing
     * AND mark all missing days as leave
     */
    private void handlePreviousDayClose(HrEntity hrEntity, LocalDate today) {
        // Step 1: Find the most recent time management record before today
        List<TimeManagement> previousRecords = timeManagementRepo.findAllBeforeDate(
                hrEntity.getHrId(),
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth());

        LocalDate startDate;

        if (!previousRecords.isEmpty()) {
            // Case 1: There are previous records
            TimeManagement mostRecentRecord = previousRecords.get(0);

            // Close the most recent record if checkout is missing
            if (mostRecentRecord.getCheckOutTime() == null) {
                Timestamp checkoutTime = determineCheckoutTime(mostRecentRecord);
                mostRecentRecord.setCheckOutTime(checkoutTime);
                calculateWorkingHours(mostRecentRecord);
                timeManagementRepo.save(mostRecentRecord);
            }

            // Start filling from the day after the most recent record
            LocalDate lastLoggedDate = LocalDate.of(
                    mostRecentRecord.getYear(),
                    mostRecentRecord.getMonth(),
                    mostRecentRecord.getDay());
            startDate = lastLoggedDate.plusDays(1);

        } else {
            // Case 2: No previous records found - go back to dateOfJoining
            if (hrEntity.getDateOfJoining() != null) {
                startDate = hrEntity.getDateOfJoining().toLocalDate();
            } else {
                // Fallback: start from yesterday if no dateOfJoining is set
                startDate = today.minusDays(1);
            }
        }

        // Step 2: Create leave entries for all missing days between startDate and today
        // (exclusive)
        LocalDate currentDate = startDate;
        while (currentDate.isBefore(today)) {
            // Check if an entry already exists for this date
            TimeManagement existingRecord = timeManagementRepo.findByDayMonthYearAndHrEntity(
                    currentDate.getDayOfMonth(),
                    currentDate.getMonthValue(),
                    currentDate.getYear(),
                    hrEntity.getHrId());

            // Only create if no record exists
            if (existingRecord == null) {
                createLeaveEntry(hrEntity, currentDate);
            }

            currentDate = currentDate.plusDays(1);
        }
    }

    /**
     * Create a leave entry for a specific date
     */
    private void createLeaveEntry(HrEntity hrEntity, LocalDate date) {
        TimeManagement leaveRecord = new TimeManagement();
        leaveRecord.setHrEntity(hrEntity);
        leaveRecord.setDay(date.getDayOfMonth());
        leaveRecord.setMonth(date.getMonthValue());
        leaveRecord.setYear(date.getYear());
        leaveRecord.setCreatedOn(Timestamp.valueOf(LocalDateTime.now()));
        leaveRecord.setIsOnLeave(true);
        leaveRecord.setIsPresent(false);
        leaveRecord.setIsHalfDay(false);
        leaveRecord.setTotalHoursWorked(0.0);
        leaveRecord.setEffectiveHours(0.0);
        leaveRecord.setOvertimeHours(0.0);

        timeManagementRepo.save(leaveRecord);
    }

    /**
     * Determine checkout time from available timestamps
     */
    private Timestamp determineCheckoutTime(TimeManagement record) {
        if (record.getBreakEndTime() != null) {
            return record.getBreakEndTime();
        } else if (record.getBreakStartTime() != null) {
            return record.getBreakStartTime();
        } else if (record.getCheckInTime() != null) {
            return record.getCheckInTime();
        }
        return new Timestamp(System.currentTimeMillis());
    }

    /**
     * Calculate total hours, effective hours, overtime, and set half-day flag
     */
    private void calculateWorkingHours(TimeManagement record) {
        if (record.getCheckInTime() == null || record.getCheckOutTime() == null) {
            return;
        }

        long checkInTime = record.getCheckInTime().getTime();
        long checkOutTime = record.getCheckOutTime().getTime();
        long totalMillis = checkOutTime - checkInTime;
        double totalHours = totalMillis / (1000.0 * 60 * 60);
        record.setTotalHoursWorked(totalHours);

        // Calculate break duration
        double breakHours = 0;
        if (record.getBreakStartTime() != null && record.getBreakEndTime() != null) {
            long breakMillis = record.getBreakEndTime().getTime() - record.getBreakStartTime().getTime();
            breakHours = breakMillis / (1000.0 * 60 * 60);
        }

        // Calculate effective hours (total - break)
        double effectiveHours = totalHours - breakHours;
        record.setEffectiveHours(Math.max(0, effectiveHours));

        // Calculate overtime (effective hours > 8)
        double overtimeHours = Math.max(0, effectiveHours - WORK_HOURS_THRESHOLD);
        record.setOvertimeHours(overtimeHours);

        // Set half-day flag if effective hours < 8
        record.setIsHalfDay(effectiveHours < WORK_HOURS_THRESHOLD);
    }

    /**
     * Build attendance response with record details
     */
    private ResponseEntity<?> buildAttendanceResponse(TimeManagement record, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("timeManagementId", record.getTimeManagementId());
        response.put("checkInTime", record.getCheckInTime());
        response.put("breakStartTime", record.getBreakStartTime());
        response.put("breakEndTime", record.getBreakEndTime());
        response.put("checkOutTime", record.getCheckOutTime());
        response.put("totalHoursWorked", record.getTotalHoursWorked());
        response.put("effectiveHours", record.getEffectiveHours());
        response.put("overtimeHours", record.getOvertimeHours());
        response.put("isHalfDay", record.getIsHalfDay());
        response.put("isPresent", record.getIsPresent());

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<?> bulkRegularize(List<BulkRegularizationRequestDto> bulkRegularizationRequestDtos) {
        if (bulkRegularizationRequestDtos.isEmpty()) {
            throw new ServiceLevelException(
                    "TimeManagementService",
                    "Bulk regularization request is empty",
                    "bulkRegularize",
                    "InvalidInput",
                    "The provided BulkRegularizationRequestDto is null or empty");
        }
        try {
            for (BulkRegularizationRequestDto bulkRegularizationRequestDto : bulkRegularizationRequestDtos) {
                HrRequest hrRequests = new HrRequest();
                hrRequests.setAppliedBy(hrEntityRepo.findById(bulkRegularizationRequestDto.getHrId()).orElseThrow(
                        () -> new ResourceNotFoundException("HrEntity", "hrId",
                                bulkRegularizationRequestDto.getHrId())));
                hrRequests.setRequestType(HrRequestType.BULK_REGULARIZATION);
                hrRequests.setAppliedOn(Timestamp.valueOf(LocalDateTime.now()));
                hrRequests.setFromDate(bulkRegularizationRequestDto.getDate());
                hrRequests.setToDate(bulkRegularizationRequestDto.getDate());
                hrRequests.setStatus(HrRequestStatus.OPEN);
                hrRequests.setRemarks(bulkRegularizationRequestDto.getReason());
                hrRequests.setCheckInHours(bulkRegularizationRequestDto.getCheckInHours());
                hrRequests.setCheckOutHours(bulkRegularizationRequestDto.getCheckOutHours());
                hrRequests.setHalfDay(bulkRegularizationRequestDto.getHalfDay());
                hrRequestRepo.save(hrRequests);
            }
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Bulk regularization requests submitted successfully");
            response.put("status", HttpStatus.OK.value());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "TimeManagementService",
                    "Error during bulk regularization",
                    "bulkRegularize",
                    e.getClass().getName(),
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> applyWeeklyOff(Long hrId, Date fromDate, Date toDate, String remarks) {
        if (ObjectUtils.isEmpty(hrId)) {
            throw new ServiceLevelException(
                    "TimeManagementService",
                    "HrId is required for applying weekly off",
                    "applyWeeklyOff",
                    "InvalidInput",
                    "The provided hrId is null or empty");
        }

        try {
            HrRequest hrRequests = new HrRequest();
            hrRequests.setAppliedBy(hrEntityRepo.findById(hrId).orElseThrow(
                    () -> new ResourceNotFoundException("HrEntity", "hrId", hrId)));
            hrRequests.setRequestType(HrRequestType.WEEKLY_OFF);
            hrRequests.setAppliedOn(Timestamp.valueOf(LocalDateTime.now()));
            hrRequests.setFromDate(fromDate);
            hrRequests.setToDate(toDate);
            hrRequests.setStatus(HrRequestStatus.OPEN);
            hrRequests.setRemarks(remarks);
            hrRequestRepo.save(hrRequests);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Weekly off request submitted successfully");
            response.put("status", HttpStatus.OK.value());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "TimeManagementService",
                    "Error during weekly off application",
                    "applyWeeklyOff",
                    e.getClass().getName(),
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> applyHoliday(Long hrId, Date fromDate, Date toDate, String remarks) {
        if (ObjectUtils.isEmpty(hrId)) {
            throw new ServiceLevelException(
                    "TimeManagementService",
                    "HrId is required for applying holiday",
                    "applyHoliday",
                    "InvalidInput",
                    "The provided hrId is null or empty");
        }

        try {
            HrRequest hrRequests = new HrRequest();
            hrRequests.setAppliedBy(hrEntityRepo.findById(hrId).orElseThrow(
                    () -> new ResourceNotFoundException("HrEntity", "hrId", hrId)));
            hrRequests.setRequestType(HrRequestType.LEAVE_APPLICATION);
            hrRequests.setAppliedOn(Timestamp.valueOf(LocalDateTime.now()));
            hrRequests.setFromDate(fromDate);
            hrRequests.setToDate(toDate);
            hrRequests.setStatus(HrRequestStatus.OPEN);
            hrRequests.setRemarks(remarks);
            hrRequestRepo.save(hrRequests);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Holiday request submitted successfully");
            response.put("status", HttpStatus.OK.value());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "TimeManagementService",
                    "Error during holiday application",
                    "applyHoliday",
                    e.getClass().getName(),
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getEmployeesAttendance(Map<String, Object> requestBody) {

        @SuppressWarnings("unchecked")
        List<Long> empIds = (List<Long>) requestBody.get("empIds");
        String date = (String) requestBody.get("date");

        if (ObjectUtils.isEmpty(empIds)) {
            throw new ServiceLevelException(
                    "TimeManagementService",
                    "Employee IDs are required to fetch attendance",
                    "getEmployeesAttendance",
                    "InvalidInput",
                    "The provided list of employee IDs is null or empty");
        }

        if (ObjectUtils.isEmpty(date)) {
            throw new ServiceLevelException(
                    "TimeManagementService",
                    "Date is required to fetch attendance",
                    "getEmployeesAttendance",
                    "InvalidInput",
                    "The provided date is null or empty. Expected format: YYYY-MM-DD (e.g., 2026-04-03)");
        }

        try {
            // Parse date with validation
            LocalDate parsedDate;
            try {
                parsedDate = LocalDate.parse(date.trim());
            } catch (Exception e) {
                throw new ServiceLevelException(
                        "TimeManagementService",
                        "Invalid date format",
                        "getEmployeesAttendance",
                        "InvalidInput",
                        "Date must be in YYYY-MM-DD format (e.g., 2026-04-03). Received: " + date);
            }

            // Fetch HrEntity records for the given employee IDs
            List<HrEntity> hrEntities = hrEntityRepo.findAllById(empIds);

            if (hrEntities.isEmpty()) {
                return ResponseEntity.ok(List.of());
            }

            // Extract HrIds from HrEntities
            List<Long> hrIds = hrEntities.stream()
                    .map(HrEntity::getHrId)
                    .toList();

            // Fetch all TimeManagement records for these employees
            List<TimeManagement> timeManagements = timeManagementRepo.findAllByDateAndHrEntityIdIn(
                    parsedDate.getDayOfMonth(),
                    parsedDate.getMonthValue(),
                    parsedDate.getYear(),
                    hrIds);

            // Map TimeManagement records to AttendanceResponse
            List<AttendanceResponse> attendanceResponses = timeManagements.stream()
                    .map(tm -> {
                        AttendanceResponse response = new AttendanceResponse();

                        // Set date from TimeManagement record
                        response.setDate(LocalDate.of(tm.getYear(), tm.getMonth(), tm.getDay()));

                        // Set employee ID from associated HrEntity
                        response.setEmployeeId(tm.getHrEntity().getEmployeeId());

                        // Set attendance timestamps
                        response.setCheckInTime(tm.getCheckInTime());
                        response.setCheckOutTime(tm.getCheckOutTime());

                        // Set total hours worked
                        response.setTotalHoursWorked(tm.getTotalHoursWorked());

                        // Set status based on attendance flags
                        String status;
                        if (Boolean.TRUE.equals(tm.getIsOnLeave())) {
                            status = "ON_LEAVE";
                        } else if (Boolean.TRUE.equals(tm.getIsHalfDay())) {
                            status = "HALF_DAY";
                        } else if (Boolean.TRUE.equals(tm.getIsPresent())) {
                            status = "PRESENT";
                        } else {
                            status = "ABSENT";
                        }
                        response.setStatus(status);

                        return response;
                    })
                    .toList();

            return ResponseEntity.ok(attendanceResponses);

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "TimeManagementService",
                    "Error while fetching employees attendance",
                    "getEmployeesAttendance",
                    e.getClass().getName(),
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> toggleAttandenceByEmpId(Long empId) {
        if (ObjectUtils.isEmpty(empId)) {
            throw new ServiceLevelException(
                    "TimeManagementService",
                    "Employee ID is required to toggle attendance",
                    "toggleAttandenceByEmpId",
                    "InvalidInput",
                    "The provided employee ID is null or empty");
        }

        try {
            // Find HrEntity by employee ID
            HrEntity hrEntity = hrEntityRepo.findByEmployeeId(empId)
                    .orElseThrow(() -> new ResourceNotFoundException("HrEntity", "employeeId", empId));

            // Delegate to toggleAttendance using the found HrId
            return toggleAttendance(hrEntity.getHrId());

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "TimeManagementService",
                    "Error while toggling attendance by employee ID",
                    "toggleAttandenceByEmpId",
                    e.getClass().getName(),
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getThisMonthAttendance(Long empId) {
        if (ObjectUtils.isEmpty(empId)) {
            throw new ServiceLevelException(
                    "TimeManagementService",
                    "Employee ID is required to fetch this month's attendance",
                    "getThisMonthAttendance",
                    "InvalidInput",
                    "The provided employee ID is null or empty");
        }

        try {
            // Find HrEntity by employee ID
            HrEntity hrEntity = hrEntityRepo.findByEmployeeId(empId)
                    .orElseThrow(() -> new ResourceNotFoundException("HrEntity", "employeeId", empId));

            LocalDate today = LocalDate.now();
            List<TimeManagement> attendanceRecords = timeManagementRepo.findAllByMonthYearAndHrEntity(
                    today.getMonthValue(),
                    today.getYear(),
                    hrEntity.getHrId());

            Map<String, Object> response = new ConcurrentHashMap<>();
            // no of days present, no of days absent, no of half days, total overtime hours
            long daysPresent = attendanceRecords.stream().filter(tm -> Boolean.TRUE.equals(tm.getIsPresent())).count();
            long daysAbsent = attendanceRecords.stream().filter(tm -> Boolean.FALSE.equals(tm.getIsPresent()) && Boolean.FALSE.equals(tm.getIsOnLeave())).count();
            long halfDays = attendanceRecords.stream().filter(tm -> Boolean.TRUE.equals(tm.getIsHalfDay())).count();
            double totalOvertimeHours = attendanceRecords.stream().mapToDouble(TimeManagement::getOvertimeHours).sum();

            calculateDeductionsAndAdditionsToPayroll(
                    response, daysPresent, daysAbsent, halfDays, totalOvertimeHours, hrEntity
            );

            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "TimeManagementService",
                    "Error while fetching this month's attendance",
                    "getThisMonthAttendance",
                    e.getClass().getName(),
                    e.getMessage());
        }

    }

    @Override
    public ResponseEntity<?> attendanceQuickUpdate(Long empId) {
        if (ObjectUtils.isEmpty(empId)) {
            throw new ServiceLevelException(
                    "TimeManagementService",
                    "Employee ID is required for attendance quick update",
                    "attendanceQuickUpdate",
                    "InvalidInput",
                    "The provided employee ID is null or empty");
        }

        try {
            // Find HrEntity by employee ID
            HrEntity hrEntity = hrEntityRepo.findByEmployeeId(empId)
                    .orElseThrow(() -> new ResourceNotFoundException("HrEntity", "employeeId", empId));

            LocalDate today = LocalDate.now();
            TimeManagement todayRecord = timeManagementRepo.findByDayMonthYearAndHrEntity(
                    today.getDayOfMonth(),
                    today.getMonthValue(),
                    today.getYear(),
                    hrEntity.getHrId());

            if (todayRecord == null) {
                return ResponseEntity.ok(new TimeManagementQuickResponseDto());
            }

            TimeManagementQuickResponseDto timeManagementQuickResponseDto=new TimeManagementQuickResponseDto();
            if (todayRecord.getBreakEndTime()!=null){
                timeManagementQuickResponseDto.setLastCheckedInTime(todayRecord.getBreakEndTime().toString());
            }else{
                timeManagementQuickResponseDto.setLastCheckedInTime(todayRecord.getCheckInTime().toString());
            }

            if (todayRecord.getCheckOutTime()!=null){
                timeManagementQuickResponseDto.setLastCheckedOutTime(todayRecord.getCheckOutTime().toString());
            } else if (todayRecord.getBreakStartTime()!=null) {
                timeManagementQuickResponseDto.setLastCheckedOutTime(todayRecord.getBreakStartTime().toString());
            }

            if (todayRecord.getBreakStartTime()!=null){
                if (todayRecord.getBreakEndTime()!=null){
                    timeManagementQuickResponseDto.setTotalBreakTime(Duration.between(todayRecord.getBreakStartTime().toLocalDateTime(), todayRecord.getBreakEndTime().toLocalDateTime()).toMinutes() + "");
                }else{
                    timeManagementQuickResponseDto.setTotalBreakTime(Duration.between(todayRecord.getBreakStartTime().toLocalDateTime(), LocalDateTime.now()).toMinutes() + "");
                }
            }

            if (todayRecord.getCheckInTime()!=null){
                if (todayRecord.getCheckOutTime()!=null){
                    timeManagementQuickResponseDto.setTotalWorkHours(Duration.between(todayRecord.getCheckInTime().toLocalDateTime(), todayRecord.getCheckOutTime().toLocalDateTime()).toMinutes() + "");
                }
                else{
                    timeManagementQuickResponseDto.setTotalWorkHours(Duration.between(todayRecord.getCheckInTime().toLocalDateTime(), LocalDateTime.now()).toMinutes() + "");
                }
            }
            return ResponseEntity.ok(timeManagementQuickResponseDto);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "TimeManagementService",
                    "Error during attendance quick update",
                    "attendanceQuickUpdate",
                    e.getClass().getName(),
                    e.getMessage());
        }
    }

    private void calculateDeductionsAndAdditionsToPayroll(Map<String, Object> response, long daysPresent, long daysAbsent, long halfDays, double totalOvertimeHours, HrEntity hrEntity) {
        try {
            Compensation compensation = hrEntity.getCompensation();
            response.put("daysPresent", daysPresent);
            response.put("daysAbsent", daysAbsent);
            response.put("halfDays", halfDays);
            response.put("totalOvertimeHours", totalOvertimeHours);

            calculateDeductionsAndAdditionMonthly(response, daysPresent, daysAbsent, halfDays, totalOvertimeHours, compensation);

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "TimeManagementService",
                    "Error while calculating deductions and additions to payroll",
                    "calculateDeductionsAndAdditionsToPayroll",
                    e.getClass().getName(),
                    e.getMessage());
        }
    }

    private void calculateDeductionsAndAdditionMonthly(Map<String, Object> response, long daysPresent, long daysAbsent, long halfDays, double totalOvertimeHours, Compensation compensation) {
        Double basePay = compensation.getBasePay();
        // calculate on basePay
        double dailyRate = basePay / 30; // Assuming 30 days in a month for simplicity
        double halfDayDeduction = (dailyRate / 2) * halfDays;
        double absenceDeduction = dailyRate * daysAbsent;
        double overtimeAddition = (dailyRate / 8) * totalOvertimeHours; // Assuming 8 working hours in a day

        double totalDeductions = halfDayDeduction + absenceDeduction;
        response.put("totalDeductions", totalDeductions);
        response.put("totalAdditions", overtimeAddition);
    }
}
