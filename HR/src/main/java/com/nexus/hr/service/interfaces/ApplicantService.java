package com.nexus.hr.service.interfaces;

import com.nexus.hr.model.entities.Applicant;
import com.nexus.hr.model.entities.ApplicantEducation;
import com.nexus.hr.model.entities.ApplicantExperience;
import com.nexus.hr.model.entities.ApplicantSkill;
import com.nexus.hr.model.enums.ApplicationStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public interface ApplicantService {

    ResponseEntity<?> createApplicant(Applicant applicant);

    ResponseEntity<?> createApplicantWithDocuments(
            Long recruitmentId, Applicant applicant,
            MultipartFile resume,
            MultipartFile coverLetter);

    ResponseEntity<?> getApplicantById(Long id);

    ResponseEntity<?> getAllApplicants(
            Long recruitmentId, ApplicationStatus status,
            String name,
            Character gender,
            Integer minAge,
            Integer maxAge,
            LocalDate appliedFromDate,
            LocalDate appliedToDate,
            Double yearsOfExperience,
            Pageable pageRequest
    );

    ResponseEntity<?> updateApplicant(@Valid Applicant applicant, Long userId);

    ResponseEntity<?> deleteApplicant(Long id);

    ResponseEntity<?> createApplicantWithoutDocuments(@Valid Applicant applicant);

    ResponseEntity<?> createApplicantEducation(@Valid ApplicantEducation applicantEducation, Long userId);

    ResponseEntity<?> createApplicantExperience(@Valid ApplicantExperience applicantExperience, Long userId);

    ResponseEntity<?> createApplicantSkill(@Valid ApplicantSkill applicantSkill, Long userId);

    ResponseEntity<?> getApplicantByUserId(Long userId);

    ResponseEntity<?> addApplicantDocument(MultipartFile document, Long userId);

    ResponseEntity<?> deleteApplicantDocument(Long userId, Long hrDocumentId);
}

