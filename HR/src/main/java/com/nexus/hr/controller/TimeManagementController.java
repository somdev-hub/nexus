package com.nexus.hr.controller;

import com.nexus.hr.exception.UnauthorizedException;
import com.nexus.hr.payload.ErrorResponseDto;
import com.nexus.hr.service.interfaces.TimeManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hr/time-management")
@RequiredArgsConstructor
public class TimeManagementController {

    private final TimeManagementService timeManagementService;

    @GetMapping("/toggle-attendance")
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

}
