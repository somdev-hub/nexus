package com.nexus.iam.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public interface RecruitmentService {
    ResponseEntity<?> createRecruitment(String recruitment, Long empId);

    ResponseEntity<?> getAllRecruitments(Long orgId, Boolean isActive, Long empId, String hiringType, String hiringStatus, Integer pageNo, Integer pageOffset);

    ResponseEntity<?> updateRecruitment(String recruitment, Long empId);

    ResponseEntity<?> getRecruitment(Long id);

    ResponseEntity<?> getClosedRecruitments(Long orgId, Integer pageNo, Integer pageOffset);

    ResponseEntity<?> createApplicantWithDocuments(Long recruitmentId, String applicant, MultipartFile resume, MultipartFile coverLetter);

    ResponseEntity<?> getAllApplicants(Long recruitmentId, String status, String name, Character gender, Integer minAge, Integer maxAge, LocalDate appliedFromDate, LocalDate appliedToDate, Integer yearsOfExperience, Integer pageNo, Integer pageSize);

    ResponseEntity<?> getApplicantById(Long id);

    ResponseEntity<?> getRecruitmentAnalytics(Long orgId);

    ResponseEntity<?> addApplicantEducation(String education, Long applicantId);

    ResponseEntity<?> addApplicantExperience(String experience, Long applicantId);

    ResponseEntity<?> addApplicantSkill(String skill, Long applicantId);

    ResponseEntity<?> updateApplicant(String payload, Long userId);

    ResponseEntity<?> getOpeningsToday(Integer pageNo, Integer pageOffset, String status, String orgName, String location, String query);

    ResponseEntity<?> getOpeningsBeforeToday(Integer pageNo, Integer pageOffset, String status, String orgName, String location, String query);

    ResponseEntity<?> getPositionPieGraph();

    ResponseEntity<?> getExperienceWiseOpenings();

    ResponseEntity<?> getCompanyWiseOpeningCount(Integer pageNo, Integer pageOffset);

    ResponseEntity<?> getRecruitmentApplicantView(Long id);

    ResponseEntity<?> getRecruitmentFilters();

    ResponseEntity<?> getRecruitmentByName(String name, Integer pageNo, Integer pageOffset);

    ResponseEntity<?> addApplicantDocument(MultipartFile document, Long userId);

    ResponseEntity<?> deleteApplicantDocument(Long userId, Long hrDocumentId);

    ResponseEntity<?> applyApplicantRecruitment(String application);

    ResponseEntity<?> hasApplicantApplied(Long recruitmentId, Long userId);

    ResponseEntity<?> getApplicantApplications(Long userId, Integer pageNo, Integer pageOffset, String status);

    ResponseEntity<?> getApplicantApplicationWithStatus(Long userId, Long recruitmentId);
}
