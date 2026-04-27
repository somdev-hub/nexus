package com.nexus.hr.controller;

import com.nexus.hr.annotation.LogActivity;
import com.nexus.hr.model.enums.HrRequestStatus;
import com.nexus.hr.model.enums.HrRequestType;
import com.nexus.hr.payload.HrRequestDto;
import com.nexus.hr.service.interfaces.HrRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hr/requests")
@RequiredArgsConstructor
public class HrRequestController {

    private final HrRequestService hrRequestService;

    @PostMapping("/")
    public ResponseEntity<?> createHrRequest(@RequestBody HrRequestDto hrRequestDto) {
        return hrRequestService.createHrRequest(hrRequestDto);
    }

    @LogActivity("Take Action on HR Requests")
    @PostMapping("/action")
    public ResponseEntity<?> takeActionForHrRequests(@RequestParam Long requestId, @RequestParam HrRequestStatus action,
                                                     @RequestParam String resolutionRemarks, @RequestParam(required = false) Long userId) {
        return hrRequestService.takeActionForHrRequests(requestId, action, resolutionRemarks, userId);
    }

    @LogActivity("Get All HR Requests")
    @GetMapping("/all")
    public ResponseEntity<?> getAllHrRequests(
            @RequestParam Long orgId,
            @RequestParam(required = false) HrRequestType requestType,
            @RequestParam(required = false) HrRequestStatus status,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "offset", required = false, defaultValue = "10") Integer offset) {
        return hrRequestService.getAllHrRequests(orgId, requestType, status, PageRequest.of(page, offset));
    }

    @GetMapping("/closed")
    public ResponseEntity<?> getAllClosedRequests(
            @RequestParam Long orgId,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer offset
    ) {
        return hrRequestService.getAllClosedRequests(orgId, PageRequest.of(page, offset));
    }

    @GetMapping("/insights")
    public ResponseEntity<?> getAllInsightRequests(@RequestParam Long orgId){
        return hrRequestService.getHrRequestInsights(orgId);
    }

    @GetMapping("/today")
    public ResponseEntity<?> getAllTodayRequests(@RequestParam Long orgId, @RequestParam(required = false, defaultValue = "0") Integer page, @RequestParam(required = false, defaultValue = "10") Integer offset, @RequestParam(required = false) HrRequestStatus status, @RequestParam(required = false) Long empId){
        return hrRequestService.getAllTodayRequests(orgId, PageRequest.of(page, offset), status, empId);
    }
}
