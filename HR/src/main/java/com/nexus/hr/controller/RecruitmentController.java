package com.nexus.hr.controller;

import com.nexus.hr.model.entities.Recruitment;
import com.nexus.hr.model.enums.ApplicationStatus;
import com.nexus.hr.model.enums.HiringStatus;
import com.nexus.hr.model.enums.HiringType;
import com.nexus.hr.payload.ApplicantApplication;
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

    @GetMapping("/analytics")
    public ResponseEntity<?> getRecruitmentAnalytics(@RequestParam Long orgId) {
        return recruitmentService.getRecruitmentAnalytics(orgId);
    }

    // nexus-direct applicant level apis

    @GetMapping("/openings-today")
    public ResponseEntity<?> getOpeningsToday(@RequestParam(required = false, defaultValue = "0") Integer pageNo, @RequestParam(required = false, defaultValue = "10") Integer pageOffset, @RequestParam(required = false) HiringStatus status, @RequestParam(required = false) String orgName, @RequestParam(required = false) String location, @RequestParam(required = false) String query) {
        return recruitmentService.getOpeningsToday(pageNo, pageOffset, status, orgName, location, query);
    }

    @GetMapping("/openings-before-today")
    public ResponseEntity<?> getOpeningsBeforeToday(@RequestParam(required = false, defaultValue = "0") Integer pageNo, @RequestParam(required = false, defaultValue = "10") Integer pageOffset, @RequestParam(required = false) HiringStatus status, @RequestParam(required = false) String orgName, @RequestParam(required = false) String location, @RequestParam(required = false) String query) {
        return recruitmentService.getOpeningsBeforeToday(pageNo, pageOffset, status, orgName, location, query);
    }

    @GetMapping("/position-pie-graph")
    public ResponseEntity<?> getPositionPieGraph() {
        return recruitmentService.getPositionPieGraph();
    }

    @GetMapping("/openings-experience-wise")
    public ResponseEntity<?> getExperienceWiseOpenings(){
        return recruitmentService.getExperienceWiseOpenings();
    }

    @GetMapping("/company-wise-opening-count")
    public ResponseEntity<?> getCompanyWiseOpeningCount(@RequestParam(required = false, defaultValue = "0") Integer pageNo, @RequestParam(required = false, defaultValue = "10") Integer pageOffset){
        return recruitmentService.getCompanyWiseOpeningCount(pageNo, pageOffset);
    }

    @GetMapping("/applicant-view/{id}")
    public ResponseEntity<?> getRecruitmentApplicantView(@PathVariable Long id) {
        return recruitmentService.getRecruitmentApplicantView(id);
    }

    @GetMapping("/filter")
    public ResponseEntity<?> getRecruitmentFilters(){
        return recruitmentService.getRecruitmentFilters();
    }

    @GetMapping("/name")
    public ResponseEntity<?> getRecruitmentByName(@RequestParam String name, @RequestParam(required = false, defaultValue = "0") Integer pageNo, @RequestParam(required = false, defaultValue = "10") Integer pageOffset) {
        return recruitmentService.getRecruitmentByName(name, pageNo, pageOffset);
    }

    @PostMapping("/applicant/apply")
    public ResponseEntity<?> applyApplicantRecruitment(@RequestBody ApplicantApplication application) {
        return recruitmentService.applyApplicantRecruitment(application);
    }

    @GetMapping("/has-applied")
    public ResponseEntity<?> hasApplicantApplied(@RequestParam Long recruitmentId, @RequestParam Long userId) {
        return recruitmentService.hasApplicantApplied(recruitmentId, userId);
    }

    @GetMapping("/applicant/applications")
    public ResponseEntity<?> getApplicantApplications(@RequestParam Long userId,  @RequestParam(required = false, defaultValue = "0") Integer pageNo, @RequestParam(required = false, defaultValue = "10") Integer pageOffset, @RequestParam(required = false) ApplicationStatus status) {
        return recruitmentService.getApplicantApplications(userId, pageNo, pageOffset, status);
    }

}
