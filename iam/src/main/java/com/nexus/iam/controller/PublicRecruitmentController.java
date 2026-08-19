package com.nexus.iam.controller;

import com.nexus.iam.service.RecruitmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/iam/public/recruitment")
public class PublicRecruitmentController {

    private final RecruitmentService recruitmentService;

    @GetMapping("/applicant-view/{id}")
    public ResponseEntity<?> getPublicRecruitmentApplicantView(@PathVariable Long id) {
        return recruitmentService.getRecruitmentApplicantView(id);
    }

    @GetMapping("/filter")
    public ResponseEntity<?> getPublicRecruitmentFilters() {
        return recruitmentService.getRecruitmentFilters();
    }

    @GetMapping("/openings-today")
    public ResponseEntity<?> getPublicOpeningsToday(
            @RequestParam(required = false, defaultValue = "0") Integer pageNo,
            @RequestParam(required = false, defaultValue = "10") Integer pageOffset,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orgName,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String query) {
        return recruitmentService.getOpeningsToday(pageNo, pageOffset, status, orgName, location, query);
    }

    @GetMapping("/openings-before-today")
    public ResponseEntity<?> getPublicOpeningsBeforeToday(
            @RequestParam(required = false, defaultValue = "0") Integer pageNo,
            @RequestParam(required = false, defaultValue = "10") Integer pageOffset,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orgName,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String query) {
        return recruitmentService.getOpeningsBeforeToday(pageNo, pageOffset, status, orgName, location, query);
    }

    @GetMapping("/position-pie-graph")
    public ResponseEntity<?> getPublicPositionPieGraph() {
        return recruitmentService.getPositionPieGraph();
    }

    @GetMapping("/openings-experience-wise")
    public ResponseEntity<?> getPublicOpeningsExperienceWise() {
        return recruitmentService.getExperienceWiseOpenings();
    }

    @GetMapping("/company-wise-opening-count")
    public ResponseEntity<?> getPublicCompanyWiseOpeningCount(
            @RequestParam(required = false, defaultValue = "0") Integer pageNo,
            @RequestParam(required = false, defaultValue = "10") Integer pageOffset) {
        return recruitmentService.getCompanyWiseOpeningCount(pageNo, pageOffset);
    }

    @GetMapping("/search")
    public ResponseEntity<?> getPublicRecruitmentSearch(
            @RequestParam String name,
            @RequestParam(required = false, defaultValue = "0") Integer pageNo,
            @RequestParam(required = false, defaultValue = "10") Integer pageOffset) {
        return recruitmentService.getRecruitmentByName(name, pageNo, pageOffset);
    }

    @GetMapping("/name")
    public ResponseEntity<?> getPublicRecruitmentByName(
            @RequestParam String name,
            @RequestParam(required = false, defaultValue = "0") Integer pageNo,
            @RequestParam(required = false, defaultValue = "10") Integer pageOffset) {
        return recruitmentService.getRecruitmentByName(name, pageNo, pageOffset);
    }

    
}