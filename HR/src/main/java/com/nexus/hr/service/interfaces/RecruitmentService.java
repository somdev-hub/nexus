package com.nexus.hr.service.interfaces;

import com.nexus.hr.model.entities.Recruitment;
import com.nexus.hr.model.enums.HiringStatus;
import com.nexus.hr.model.enums.HiringType;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

public interface RecruitmentService {
    ResponseEntity<?> createRecruitment(Recruitment recruitment, Long empId);

    ResponseEntity<?> getRecruitment(Long id);

    ResponseEntity<?> getAllRecruitments(Boolean isActive, Long empId, HiringType hiringType, HiringStatus hiringStatus, PageRequest pageRequest);

    ResponseEntity<?> updateRecruitment(@Valid Recruitment recruitment, Long empId);
}
