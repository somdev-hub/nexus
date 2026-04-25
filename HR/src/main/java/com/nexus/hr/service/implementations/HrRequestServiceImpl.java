package com.nexus.hr.service.implementations;

import com.nexus.hr.exception.ResourceNotFoundException;
import com.nexus.hr.exception.ServiceLevelException;
import com.nexus.hr.model.entities.*;
import com.nexus.hr.model.enums.HrRequestStatus;
import com.nexus.hr.model.enums.HrRequestType;
import com.nexus.hr.model.enums.LeaveStatus;
import com.nexus.hr.payload.HrRequestDto;
import com.nexus.hr.payload.RestPayload;
import com.nexus.hr.payload.response.HrResponseDto;
import com.nexus.hr.repository.*;
import com.nexus.hr.service.interfaces.HrRequestService;
import com.nexus.hr.utils.CommonConstants;
import com.nexus.hr.utils.CommonUtils;
import com.nexus.hr.utils.RestServices;
import com.nexus.hr.utils.WebConstants;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HrRequestServiceImpl implements HrRequestService {

    private final HrRequestRepo hrRequestRepo;
    private final ModelMapper modelMapper;
    private final CommonUtils commonUtils;
    private final RestServices restServices;
    private final WebConstants webConstants;
    private final HrEntityRepo hrEntityRepo;
    private final EmployeeLeaveAllocationRepo employeeLeaveAllocationRepo;
    private final EmployeeLeavesRepo employeeLeavesRepo;
    private final TimeManagementRepo timeManagementRepo;
    private final PositionRepository positionRepository;

    @Override
    @Transactional
    public ResponseEntity<?> takeActionForHrRequests(Long requestId, HrRequestStatus action, String resolutionRemarks, Long userId) {
        if (ObjectUtils.isEmpty(requestId)) {
            throw new ServiceLevelException("HR Service", "Request ID cannot be null or empty",
                    "takeActionForHrRequests", "InvalidInput", "Request ID is null or empty");
        }
        try {
            HrRequest hrRequest = hrRequestRepo.findById(requestId)
                    .orElseThrow(() -> new ResourceNotFoundException("HrRequests", "requestId", requestId));

            if (HrRequestType.LEAVE_APPLICATION.equals(hrRequest.getRequestType()) && HrRequestStatus.APPROVED.equals(action)) {
                EmployeeLeaveAllocation employeeLeaveAllocation = employeeLeaveAllocationRepo.findByHrEntity_HrIdAndLeaveTypeAndYear(hrRequest.getAppliedBy().getHrId(), hrRequest.getLeaveType(), LocalDate.now().getYear()).orElseThrow(() -> new ResourceNotFoundException("HrRequests", "requestId", requestId));
                Double calculatedLeaveDays = calculateLeaveDays(hrRequest);
                if (employeeLeaveAllocation.getRemainingDays() < calculatedLeaveDays) {
                    throw new ServiceLevelException("HR Service", "Insufficient leave balance for approval",
                            "takeActionForHrRequests", "InsufficientBalance", "Employee does not have enough leave balance for this request");
                }
                employeeLeaveAllocation.setUsedDays(employeeLeaveAllocation.getUsedDays() + calculatedLeaveDays);
                employeeLeaveAllocation.setRemainingDays(employeeLeaveAllocation.getRemainingDays() - calculatedLeaveDays);
                employeeLeaveAllocationRepo.save(employeeLeaveAllocation);

                EmployeeLeaves employeeLeave = new EmployeeLeaves();
                employeeLeave.setHrEntity(hrRequest.getAppliedBy());
                employeeLeave.setLeaveType(hrRequest.getLeaveType());
                employeeLeave.setLeaveStatus(LeaveStatus.APPROVED);
                employeeLeave.setAppliedDate(new Timestamp(System.currentTimeMillis()));
                employeeLeave.setStartDate(hrRequest.getFromDate());
                employeeLeave.setEndDate(hrRequest.getToDate());
                employeeLeave.setNumberOfDays(calculatedLeaveDays);
                employeeLeave.setReason(hrRequest.getRemarks());
                employeeLeave.setApprovedOrRevokedDate(new Timestamp(System.currentTimeMillis()));
                employeeLeave.setApprovedBy(userId);
                employeeLeavesRepo.save(employeeLeave);
            }
            if (HrRequestType.BULK_REGULARIZATION.equals(hrRequest.getRequestType()) && HrRequestStatus.APPROVED.equals(action)) {
                TimeManagement timeManagement = new TimeManagement();
                timeManagement.setCreatedOn(new Timestamp(System.currentTimeMillis()));
                timeManagement.setDay(hrRequest.getFromDate().toLocalDate().getDayOfMonth());
                timeManagement.setMonth(hrRequest.getFromDate().toLocalDate().getMonthValue());
                timeManagement.setYear(hrRequest.getFromDate().toLocalDate().getYear());
                timeManagement.setCheckInTime(hrRequest.getCheckInHours());
                timeManagement.setCheckOutTime(hrRequest.getCheckOutHours());
                timeManagement.setIsPresent(true);
                timeManagement.setBreakStartTime(
                        new Timestamp(hrRequest.getCheckInHours().getTime() + 4 * 60 * 60 * 1000)
                ); // add four hours
                timeManagement.setBreakEndTime(
                        new Timestamp(hrRequest.getCheckOutHours().getTime() + 5 * 60 * 60 * 1000)
                );
                timeManagement.setIsHalfDay(hrRequest.getHalfDay());
                timeManagement.setTotalHoursWorked(
                        (double) ((hrRequest.getCheckOutHours().getTime() - hrRequest.getCheckInHours().getTime()) / (1000 * 60 * 60))
                );
                timeManagement.setOvertimeHours(
                        Math.max(0, timeManagement.getTotalHoursWorked() - 8)
                );
                timeManagement.setEffectiveHours(
                        timeManagement.getTotalHoursWorked() - 1
                ); // total hours worked minus 1 hour break
                timeManagement.setHrEntity(hrRequest.getAppliedBy());
                timeManagementRepo.save(timeManagement);
            }
            hrRequest.setStatus(action);
            hrRequest.setResolutionRemarks(resolutionRemarks);
            hrRequest.setResolvedOn(new Timestamp(System.currentTimeMillis()));


            // kafka implementation

            hrRequestRepo.save(hrRequest);
            return ResponseEntity
                    .ok("HR request with ID " + requestId + " has been " + action.name().toLowerCase() + ".");
        } catch (RuntimeException e) {
            throw new ServiceLevelException("HR Service", "Exception occurred while taking action on HR request",
                    "takeActionForHrRequests", e.getClass().getName(), e.getMessage());
        }
    }

    private Double calculateLeaveDays(HrRequest hrRequest) {
        if (!HrRequestType.LEAVE_APPLICATION.equals(hrRequest.getRequestType())) {
            return 0.0;
        }
        Date fromDate = hrRequest.getFromDate();
        Date toDate = hrRequest.getToDate();

        // calculate days only
        long daysBetween = (toDate.getTime() - fromDate.getTime()) / (1000 * 60 * 60 * 24) + 1; // +1 to include both start and end date
        return (double) daysBetween;
    }

    @Override
    public ResponseEntity<?> getAllHrRequests(Long orgId, HrRequestType requestType, HrRequestStatus status, Pageable pageable) {
        try {
            Page<HrRequest> hrRequestsPage;
            if (!ObjectUtils.isEmpty(requestType) && !ObjectUtils.isEmpty(status)) {
                hrRequestsPage = hrRequestRepo.findAllByHrRequestTypeAndStatusAndOrgId(orgId, requestType, status, pageable);
            } else if (!ObjectUtils.isEmpty(requestType)) {
                hrRequestsPage = hrRequestRepo.findAllByHrRequestTypeAndOrgId(orgId, requestType, pageable);
            } else if (!ObjectUtils.isEmpty(status)) {
                hrRequestsPage = hrRequestRepo.findAllByStatusAndOrgId(orgId, status, pageable);
            } else {
                hrRequestsPage = hrRequestRepo.findAllWithOrgId(orgId, pageable);
            }
            return getResponseEntity(hrRequestsPage);
        } catch (RuntimeException e) {
            throw new ServiceLevelException("HR Service", "Exception occurred while fetching HR requests",
                    "getAllHrRequests", e.getClass().getName(), e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> createHrRequest(HrRequestDto hrRequestDto) {
        if (ObjectUtils.isEmpty(hrRequestDto)) {
            throw new ServiceLevelException("HR Service", "HR Request data cannot be null or empty",
                    "createHrRequest", "InvalidInput", "HR Request data is null or empty");
        }
        ResponseEntity<?> response;
        try {
            HrRequest hrRequest = new HrRequest();
            hrRequest.setAppliedBy(hrEntityRepo.findByEmployeeId(hrRequestDto.getEmpId()).orElseThrow(() -> new ResourceNotFoundException("HrEntities", "employeeId", hrRequestDto.getEmpId())));
            hrRequest.setRequestType(hrRequestDto.getHrRequestType());
            hrRequest.setAppliedOn(new Timestamp(System.currentTimeMillis()));
            hrRequest.setRemarks(hrRequestDto.getRemarks());
            hrRequest.setStatus(HrRequestStatus.OPEN);
            boolean isLeaveApplicationRequest = HrRequestType.LEAVE_APPLICATION.equals(hrRequestDto.getHrRequestType());
            boolean isBulkRegularizationRequest = HrRequestType.BULK_REGULARIZATION.equals(hrRequestDto.getHrRequestType());
            if (isLeaveApplicationRequest || isBulkRegularizationRequest) {
                // check if todate is before fromdate
                if (hrRequestDto.getToDate().before(hrRequestDto.getFromDate())) {
                    throw new ServiceLevelException("HR Service", "To date cannot be before from date",
                            "createHrRequest", "InvalidDateRange", "To date cannot be before from date");
                }
                hrRequest.setFromDate(hrRequestDto.getFromDate());
                hrRequest.setToDate(hrRequestDto.getToDate());
                if (isLeaveApplicationRequest) {
                    Double leaveDays = calculateLeaveDays(hrRequest);
                    employeeLeaveAllocationRepo.findByHrEntity_HrIdAndLeaveTypeAndYear(hrRequest.getAppliedBy().getHrId(), hrRequestDto.getLeaveType(), LocalDate.now().getYear()).ifPresent(allocation -> {
                        if (allocation.getRemainingDays() < leaveDays) {
                            throw new ServiceLevelException("HR Service", "Insufficient leave balance for application",
                                    "createHrRequest", "InsufficientBalance", "Employee does not have enough leave balance for this request");
                        }
                    });
                    hrRequest.setHalfDay(hrRequestDto.getHalfDay());
                    hrRequest.setLeaveType(hrRequestDto.getLeaveType());
                    hrRequest.setLeaveBalanceUsed(leaveDays);
                }
                if (isBulkRegularizationRequest) {
                    hrRequest.setCheckInHours(hrRequestDto.getCheckInHours());
                    hrRequest.setCheckOutHours(hrRequestDto.getCheckOutHours());
                }
            }
            HrRequest saved = hrRequestRepo.save(hrRequest);

            response = ResponseEntity.ok("HrRequest Created Successfully: " + saved.getRequestId());
        } catch (RuntimeException e) {
            throw new ServiceLevelException("HR Service", "Exception occurred while creating HR request",
                    "createHrRequest", e.getClass().getName(), e.getMessage());
        }

        return response;
    }

    @Override
    public ResponseEntity<?> getAllClosedRequests(Long orgId, PageRequest of) {
        try {
            Page<HrRequest> closedRequests = hrRequestRepo.findAllByOrgIdAndStatusIn(orgId, List.of(HrRequestStatus.CLOSED, HrRequestStatus.REJECTED, HrRequestStatus.APPROVED), of);
            return getResponseEntity(closedRequests);
        } catch (RuntimeException e) {
            throw new ServiceLevelException("HR Service", "Exception occurred while fetching closed HR requests",
                    "getAllClosedRequests", e.getClass().getName(), e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getHrRequestInsights(Long orgId) {
        if (ObjectUtils.isEmpty(orgId)) {
            throw new ServiceLevelException("HR Service", "Organization ID cannot be null or empty",
                    "getHrRequestInsights", "InvalidInput", "Organization ID is null or empty");
        }
        ResponseEntity<?> response;
        try {
            Map<String, Long> map = new HashMap<>();
            Long openCases = hrRequestRepo.findCountByOrgIdAndStatus(orgId, HrRequestStatus.OPEN);
            Long approvedCases = hrRequestRepo.findCountByOrgIdAndStatus(orgId, HrRequestStatus.APPROVED);
            Long rejectedCases = hrRequestRepo.findCountByOrgIdAndStatus(orgId, HrRequestStatus.REJECTED);
            Long inScrutinyCases= hrRequestRepo.findCountByOrgIdAndStatus(orgId, HrRequestStatus.SCRUTINY);
            Long allHandledCases = hrRequestRepo.findCountByOrgIdAndStatusIn(orgId, List.of(HrRequestStatus.APPROVED, HrRequestStatus.REJECTED, HrRequestStatus.CLOSED));

            map.put("openCases", openCases);
            map.put("approvedCases", approvedCases);
            map.put("rejectedCases", rejectedCases);
            map.put("inScrutinyCases", inScrutinyCases);
            map.put("allHandledCases", allHandledCases);
            response = ResponseEntity.ok(map);
        } catch (RuntimeException e) {
            throw new ServiceLevelException("HR Service", "Exception occurred while fetching HR request insights",
                    "getHrRequestInsights", e.getClass().getName(), e.getMessage());
        }
        return response;
    }

    @NonNull
    private ResponseEntity<?> getResponseEntity(Page<HrRequest> closedRequests) {
        Page<HrResponseDto> hrResponseDtos = closedRequests.map(request -> {
            HrResponseDto hrResponseDto = modelMapper.map(request, HrResponseDto.class);
            Position position = positionRepository.findByHrEntityAndIsActiveTrue(request.getAppliedBy()).orElseThrow(() -> new ResourceNotFoundException("Positions", "hrId", request.getAppliedBy().getHrId()));
            hrResponseDto.setDepartment(position.getDepartment());
            hrResponseDto.setRole(position.getTitle());
            hrResponseDto.setEmpId(request.getAppliedBy().getEmployeeId());
            RestPayload restPayload = commonUtils.buildRestPayload(webConstants.getUserDetailsUrl(),
                    Map.of("userId", request.getAppliedBy().getEmployeeId().toString()), null, CommonConstants.APPLICATION_JSON);
            ResponseEntity<?> response = restServices.hrRestCall(restPayload.getBuilder().toUriString(), null,
                    restPayload.getHeaders(), HttpMethod.GET, request.getAppliedBy().getHrId());
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, String> details = (Map<String, String>) response.getBody();
                hrResponseDto.setEmployeeName(details.get("name"));
                hrResponseDto.setEmployeeEmail(details.get("email"));
            }
            return hrResponseDto;
        });
        return ResponseEntity.ok(hrResponseDtos);
    }

}
