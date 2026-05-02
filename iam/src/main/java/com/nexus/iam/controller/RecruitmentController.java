package com.nexus.iam.controller;

import com.nexus.iam.service.RecruitmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/iam/recruitment")
public class RecruitmentController {
    private final RecruitmentService recruitmentService;

    @PostMapping("/")
    public ResponseEntity<?> createRecruitment(@RequestBody String recruitment, @RequestParam Long empId) {
        return recruitmentService.createRecruitment(recruitment, empId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRecruitmentById(@PathVariable Long id) {
        return recruitmentService.getRecruitment(id);
    }

    @GetMapping("/")
    public ResponseEntity<?> getAllRecruitments(@RequestParam Long orgId, @RequestParam(required = false) Boolean isActive, @RequestParam(required = false) Long empId, @RequestParam(required = false) String hiringType, @RequestParam(required = false) String hiringStatus, @RequestParam(required = false, defaultValue = "0") Integer pageNo, @RequestParam(required = false, defaultValue = "10") Integer pageOffset) {
        return recruitmentService.getAllRecruitments(orgId, isActive, empId, hiringType, hiringStatus, pageNo, pageOffset);
    }

    @PutMapping("/")
    public ResponseEntity<?> updateRecruitment(@RequestBody String recruitment, @RequestParam Long empId) {
        return recruitmentService.updateRecruitment(recruitment, empId);
    }

    @GetMapping("/closed")
    public ResponseEntity<?> closeRecruitment(@RequestParam Long orgId, @RequestParam(required = false, defaultValue = "0") Integer pageNo, @RequestParam(required = false, defaultValue = "10") Integer pageOffset) {
        return recruitmentService.getClosedRecruitments(orgId, pageNo, pageOffset);
    }
}
