package com.nexus.hr.service.interfaces;

import com.nexus.hr.model.enums.HrRequestStatus;
import com.nexus.hr.model.enums.HrRequestType;
import com.nexus.hr.payload.HrRequestDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public interface HrRequestService {
    ResponseEntity<?> takeActionForHrRequests(Long requestId, HrRequestStatus action, String resolutionRemarks);

    ResponseEntity<?> getAllHrRequests(Long orgId, HrRequestType requestType, HrRequestStatus status, Pageable pageable);

    ResponseEntity<?> createHrRequest(HrRequestDto hrRequestDto);

    ResponseEntity<?> getAllClosedRequests(Long orgId, PageRequest of);

    ResponseEntity<?> getHrRequestInsights(Long orgId);
}
