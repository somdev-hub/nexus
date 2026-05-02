package com.nexus.iam.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

public interface RecruitmentService {
    ResponseEntity<?> createRecruitment(String recruitment, Long empId);

    ResponseEntity<?> getAllRecruitments(Long orgId, Boolean isActive, Long empId, String hiringType, String hiringStatus, Integer pageNo, Integer pageOffset);

    ResponseEntity<?> updateRecruitment(String recruitment, Long empId);

    ResponseEntity<?> getRecruitment(Long id);

    ResponseEntity<?> getClosedRecruitments(Long orgId, Integer pageNo, Integer pageOffset);
}
