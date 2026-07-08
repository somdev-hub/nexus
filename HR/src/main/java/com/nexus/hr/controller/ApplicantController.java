package com.nexus.hr.controller;

import com.nexus.hr.model.entities.Applicant;
import com.nexus.hr.model.entities.ApplicantEducation;
import com.nexus.hr.model.entities.ApplicantExperience;
import com.nexus.hr.model.entities.ApplicantSkill;
import com.nexus.hr.model.enums.ApplicationStatus;
import com.nexus.hr.service.interfaces.ApplicantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/hr/applicants")
public class ApplicantController {

    private final ApplicantService applicantService;

    @PostMapping("/without-documents")
    public ResponseEntity<?> createApplicantWithoutDocuments(
            @Valid @RequestBody Applicant applicant) {
        return applicantService.createApplicantWithoutDocuments(applicant);
    }

    /**
     * Create a new applicant
     *
     * @param applicant Applicant entity with required fields
     * @return Created applicant
     */
    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createApplicantWithDocuments(
            @RequestPart("recruitmentId") Long recruitmentId,
            @RequestPart("applicant") Applicant applicant,
            @RequestPart(value = "resume", required = false) MultipartFile resume,
            @RequestPart(value = "coverLetter", required = false) MultipartFile coverLetter) {
        return applicantService.createApplicantWithDocuments(recruitmentId, applicant, resume, coverLetter);
    }

    /**
     * Get applicant by ID
     *
     * @param id Applicant ID
     * @return Applicant details
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getApplicantById(@PathVariable Long id) {
        return applicantService.getApplicantById(id);
    }

    /**
     * Get all applicants with optional filters and pagination
     * All filter parameters are optional and can be combined
     *
     * @param status            Application status (e.g., APPLIED, REJECTED,
     *                          ACCEPTED)
     * @param name              Search by applicant first or last name
     *                          (case-insensitive, partial match)
     * @param gender            Applicant gender (M, F, etc.)
     * @param minAge            Minimum age for filtering
     * @param maxAge            Maximum age for filtering
     * @param appliedFromDate   Start date for applied date range (yyyy-MM-dd)
     * @param appliedToDate     End date for applied date range (yyyy-MM-dd)
     * @param yearsOfExperience Minimum years of experience
     * @param pageNo            Page number (default: 0)
     * @param pageSize          Page size (default: 10)
     * @return Paginated list of applicants
     */
    @GetMapping("/")
    public ResponseEntity<?> getAllApplicants(
            @RequestParam Long recruitmentId,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Character gender,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appliedFromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appliedToDate,
            @RequestParam(required = false) Double yearsOfExperience,
            @RequestParam(required = false, defaultValue = "0") Integer pageNo,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return applicantService.getAllApplicants(
                recruitmentId,
                status,
                name,
                gender,
                minAge,
                maxAge,
                appliedFromDate,
                appliedToDate,
                yearsOfExperience,
                PageRequest.of(pageNo, pageSize));
    }

    /**
     * Update an existing applicant
     *
     * @param applicant Applicant entity with updated fields (applicantId is
     *                  required)
     * @return Updated applicant
     */
    @PutMapping("/")
    public ResponseEntity<?> updateApplicant(@Valid @RequestBody Applicant applicant, @RequestParam Long userId) {
        return applicantService.updateApplicant(applicant, userId);
    }

    /**
     * Delete an applicant
     *
     * @param id Applicant ID to delete
     * @return Success message
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteApplicant(@PathVariable Long id) {
        return applicantService.deleteApplicant(id);
    }

    @PostMapping("/education")
    public ResponseEntity<?> createApplicantEducation(@Valid @RequestBody ApplicantEducation applicantEducation,
            @RequestParam Long userId) {
        return applicantService.createApplicantEducation(applicantEducation, userId);
    }

    @PostMapping("/experience")
    public ResponseEntity<?> createApplicantExperience(@Valid @RequestBody ApplicantExperience applicantExperience,
            @RequestParam Long userId) {
        return applicantService.createApplicantExperience(applicantExperience, userId);
    }

    @PostMapping("/skill")
    public ResponseEntity<?> createApplicantSkill(@Valid @RequestBody ApplicantSkill applicantSkill,
            @RequestParam Long userId) {
        return applicantService.createApplicantSkill(applicantSkill, userId);
    }

    @GetMapping("/userId/{userId}")
    public ResponseEntity<?> getApplicantByUserId(@PathVariable Long userId) {
        return applicantService.getApplicantByUserId(userId);
    }

    @PostMapping("/document")
    public ResponseEntity<?> addApplicantDocument(@RequestParam MultipartFile document, @RequestParam Long userId) {
        return applicantService.addApplicantDocument(document, userId);
    }

}
