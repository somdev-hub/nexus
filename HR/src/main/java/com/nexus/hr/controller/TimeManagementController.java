package com.nexus.hr.controller;

import com.nexus.hr.exception.UnauthorizedException;
import com.nexus.hr.payload.ErrorResponseDto;
import com.nexus.hr.service.interfaces.TimeManagementService;
import com.nexus.hr.utils.CommonUtils;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/hr/time-management")
@RequiredArgsConstructor
public class TimeManagementController {

    private final TimeManagementService timeManagementService;
    private final CommonUtils commonUtils;

    @PostMapping("/toggle-attendance")
    public ResponseEntity<?> toggleAttendance(@RequestParam Long hrId, @RequestHeader("Authorization") String authorizationHeader) {

        if (ObjectUtils.isEmpty(hrId)) {
            throw new UnauthorizedException(
                    "TimeManagementController",
                    "HR ID is required to toggle attendance"
            );
        }
        if (ObjectUtils.isEmpty(hrId)) {
            ErrorResponseDto errorResponseDto = new ErrorResponseDto();
            errorResponseDto.setMessage("HR ID is required to toggle attendance");
            errorResponseDto.setStatusCode(401);
            return ResponseEntity.status(401).body(errorResponseDto);
        }
        return timeManagementService.toggleAttendance(hrId);
    }

    @PostMapping("/toggle-attendance/emp-id")
    public ResponseEntity<?> toggleAttandenceByEmpId(@RequestParam Long empId, @RequestHeader("Authorization") String token) {
        if (ObjectUtils.isEmpty(token) || !commonUtils.validateToken(token)) {
            throw new UnauthorizedException(
                    "TimeManagementController",
                    "Invalid or missing authorization token"
            );
        }
        if (ObjectUtils.isEmpty(empId)) {
            throw new UnauthorizedException(
                    "TimeManagementController",
                    "Employee ID is required to toggle attendance"
            );
        }
        return timeManagementService.toggleAttandenceByEmpId(empId);
    }
    

    @PostMapping("/attendance")
    public ResponseEntity<?> getEmployeesAttendance(@RequestBody Map<String, Object> requestBody, @RequestHeader("Authorization") String token) {
        if (ObjectUtils.isEmpty(token) || !commonUtils.validateToken(token)) {
            throw new UnauthorizedException(
                    "TimeManagementController",
                    "Invalid or missing authorization token"
            );
        }
        return timeManagementService.getEmployeesAttendance(requestBody);
    }

    @GetMapping("/get-this-month-attendance/{empId}")
    public ResponseEntity<?> getThisMonthAttendance(@PathVariable Long empId) {
        return timeManagementService.getThisMonthAttendance(empId);
    }
    

}
