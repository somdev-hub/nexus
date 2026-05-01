package com.nexus.hr.service.interfaces;

import com.nexus.hr.model.entities.Applicant;
import com.nexus.hr.model.enums.ApplicationStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

public interface ApplicantService {

    ResponseEntity<?> createApplicant(Applicant applicant);

    ResponseEntity<?> getApplicantById(Long id);

    ResponseEntity<?> getAllApplicants(
            ApplicationStatus status,
            String name,
            Character gender,
            Integer minAge,
            Integer maxAge,
            LocalDate appliedFromDate,
            LocalDate appliedToDate,
            Integer yearsOfExperience,
            PageRequest pageRequest
    );

    ResponseEntity<?> updateApplicant(@Valid Applicant applicant);

    ResponseEntity<?> deleteApplicant(Long id);
}

