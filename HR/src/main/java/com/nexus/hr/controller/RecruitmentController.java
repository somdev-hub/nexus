package com.nexus.hr.controller;

import com.nexus.hr.model.entities.Recruitment;
import com.nexus.hr.model.enums.HiringStatus;
import com.nexus.hr.model.enums.HiringType;
import com.nexus.hr.service.interfaces.RecruitmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/hr/recruitment")
public class RecruitmentController {

    private final RecruitmentService recruitmentService;

    @PostMapping("/")
    public ResponseEntity<?> createRecruitment(@Valid @RequestBody Recruitment recruitment, @RequestParam Long empId) {
        return recruitmentService.createRecruitment(recruitment, empId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRecruitmentById(@PathVariable Long id) {
        return recruitmentService.getRecruitment(id);
    }

    @GetMapping("/")
    public ResponseEntity<?> getAllRecruitments(@RequestParam Long orgId, @RequestParam(required = false) Boolean isActive, @RequestParam(required = false) Long empId, @RequestParam(required = false) HiringType hiringType, @RequestParam(required = false) HiringStatus hiringStatus, @RequestParam(required = false, defaultValue = "0") Integer pageNo, @RequestParam(required = false, defaultValue = "10") Integer pageOffset) {
        return recruitmentService.getAllRecruitments(orgId, isActive, empId, hiringType, hiringStatus, PageRequest.of(pageNo, pageOffset));
    }

    @PutMapping("/")
    public ResponseEntity<?> updateRecruitment(@Valid @RequestBody Recruitment recruitment, @RequestParam Long empId) {
        return recruitmentService.updateRecruitment(recruitment, empId);
    }

    @GetMapping("/closed")
    public ResponseEntity<?> closeRecruitment(@RequestParam Long orgId, @RequestParam(required = false, defaultValue = "0") Integer pageNo, @RequestParam(required = false, defaultValue = "10") Integer pageOffset) {
        return recruitmentService.getClosedRecruitments(orgId, PageRequest.of(pageNo, pageOffset));
    }
}
