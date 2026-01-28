package com.nexus.hr.controller;

import com.nexus.hr.annotation.LogActivity;
import com.nexus.hr.exception.UnauthorizedException;
import com.nexus.hr.model.enums.HrRequestStatus;
import com.nexus.hr.payload.HrInitRequestDto;
import com.nexus.hr.service.interfaces.HrService;
import com.nexus.hr.utils.CommonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for HR operations including PDF generation
 */
@Slf4j
@RestController
@RequestMapping("/hr/")
@RequiredArgsConstructor
public class HrController {

    private final HrService hrService;
    private final CommonUtils commonUtils;

    /**
     * Initialize HR for an employee and generate PDFs
     *
     * @param hrInitRequestDto Request containing employee and position details
     * @return Response containing generated PDF files
     */
    @LogActivity("Initialize HR and Generate PDFs")
    @PostMapping("/employee/init")
    public ResponseEntity<?> initializeHr(@RequestBody HrInitRequestDto hrInitRequestDto) {
        log.info("Initializing HR for employee ID: {}", hrInitRequestDto.getEmployeeId());
        try {
            ResponseEntity<?> response = hrService.initHr(hrInitRequestDto);
            log.info("HR initialization successful for employee ID: {}", hrInitRequestDto.getEmployeeId());
            return response;
        } catch (Exception e) {
            log.error("Error initializing HR for employee ID: {}", hrInitRequestDto.getEmployeeId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @LogActivity("Take Action on HR Requests")
    @PostMapping("/requests/action")
    public ResponseEntity<?> takeActionForHrRequests(@RequestParam Long requestId, @RequestParam HrRequestStatus action, @RequestParam String resolutionRemarks, @RequestHeader("Authorization") String token) {
        if (ObjectUtils.isEmpty(token) || !commonUtils.validateToken(token)) {
            throw new UnauthorizedException("Unauthorized", "Invalid or missing authorization token");
        }
        return hrService.takeActionForHrRequests(requestId, action, resolutionRemarks);
    }

    @LogActivity("Get All HR Requests")
    @GetMapping("/all/hr-requests")
    public ResponseEntity<?> getAllHrRequests(@RequestParam(value = "page", required = false, defaultValue = "0") Integer page, @RequestParam(value = "offset", required = false, defaultValue = "10") Integer offset, @RequestHeader("Authorization") String token) {
        if (ObjectUtils.isEmpty(token) || !commonUtils.validateToken(token)) {
            throw new UnauthorizedException("Unauthorized", "Invalid or missing authorization token");
        }
        return hrService.getAllHrRequests(PageRequest.of(page, offset));
    }
}
