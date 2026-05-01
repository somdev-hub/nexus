package com.nexus.hr.service.implementations;

import com.nexus.hr.exception.ResourceNotFoundException;
import com.nexus.hr.exception.ServiceLevelException;
import com.nexus.hr.model.entities.Applicant;
import com.nexus.hr.model.enums.ApplicationStatus;
import com.nexus.hr.repository.ApplicantRepo;
import com.nexus.hr.service.interfaces.ApplicantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDate;

@RequiredArgsConstructor
@Service
public class ApplicantServiceImpl implements ApplicantService {

    private final ApplicantRepo applicantRepo;

    @Override
    public ResponseEntity<?> createApplicant(Applicant applicant) {
        if (ObjectUtils.isEmpty(applicant)) {
            throw new ServiceLevelException(
                    "ApplicantService",
                    "Required applicant body missing",
                    "createApplicant",
                    "Missing required data exception",
                    "Required data applicant is missing"
            );
        }
        try {
            Applicant savedApplicant = applicantRepo.save(applicant);
            return ResponseEntity.ok(savedApplicant);
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "ApplicantService",
                    "Error occurred while creating applicant",
                    "createApplicant",
                    "Service level exception",
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> getApplicantById(Long id) {
        if (ObjectUtils.isEmpty(id)) {
            throw new ServiceLevelException(
                    "ApplicantService",
                    "Applicant id is missing",
                    "getApplicantById",
                    "Missing required data exception",
                    "Required data applicant id is missing"
            );
        }
        try {
            Applicant applicant = applicantRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException(
                    "Applicant",
                    "id",
                    id.toString()
            ));
            return ResponseEntity.ok(applicant);
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "ApplicantService",
                    "Error occurred while fetching applicant",
                    "getApplicantById",
                    "Service level exception",
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> getAllApplicants(
            ApplicationStatus status,
            String name,
            Character gender,
            Integer minAge,
            Integer maxAge,
            LocalDate appliedFromDate,
            LocalDate appliedToDate,
            Integer yearsOfExperience,
            PageRequest pageRequest
    ) {
        try {
            // Validate pageRequest
            if (ObjectUtils.isEmpty(pageRequest)) {
                throw new ServiceLevelException(
                        "ApplicantService",
                        "PageRequest is required",
                        "getAllApplicants",
                        "Missing required data exception",
                        "PageRequest cannot be null or empty"
                );
            }

            // Count how many filters are provided
            boolean hasStatus = !ObjectUtils.isEmpty(status);
            boolean hasName = !ObjectUtils.isEmpty(name);
            boolean hasGender = !ObjectUtils.isEmpty(gender);
            boolean hasAge = !ObjectUtils.isEmpty(minAge) && !ObjectUtils.isEmpty(maxAge);
            boolean hasAppliedDate = !ObjectUtils.isEmpty(appliedFromDate) && !ObjectUtils.isEmpty(appliedToDate);
            boolean hasYearsOfExperience = !ObjectUtils.isEmpty(yearsOfExperience);

            Page<Applicant> result;

            // No filters - return all
            if (!hasStatus && !hasName && !hasGender && !hasAge && !hasAppliedDate && !hasYearsOfExperience) {
                result = applicantRepo.findAll(pageRequest);
            }
            // Only one filter
            else if (hasStatus && !hasName && !hasGender && !hasAge && !hasAppliedDate && !hasYearsOfExperience) {
                result = applicantRepo.findByApplicationStatus(status, pageRequest);
            } else if (hasName && !hasStatus && !hasGender && !hasAge && !hasAppliedDate && !hasYearsOfExperience) {
                result = applicantRepo.findByNameContaining(name, pageRequest);
            } else if (hasGender && !hasStatus && !hasName && !hasAge && !hasAppliedDate && !hasYearsOfExperience) {
                result = applicantRepo.findByApplicantGender(gender, pageRequest);
            } else if (hasAge && !hasStatus && !hasName && !hasGender && !hasAppliedDate && !hasYearsOfExperience) {
                result = applicantRepo.findByAgeRange(minAge, maxAge, pageRequest);
            } else if (hasAppliedDate && !hasStatus && !hasName && !hasGender && !hasAge && !hasYearsOfExperience) {
                result = applicantRepo.findAppliedBetweenDates(appliedFromDate, appliedToDate, pageRequest);
            } else if (hasYearsOfExperience && !hasStatus && !hasName && !hasGender && !hasAge && !hasAppliedDate) {
                result = applicantRepo.findByYearsOfExperience(yearsOfExperience, pageRequest);
            }
            // Two filters
            else if (hasStatus && hasName) {
                result = applicantRepo.findByStatusAndName(status, name, pageRequest);
            } else if (hasStatus && hasGender) {
                result = applicantRepo.findByStatusAndGender(status, gender, pageRequest);
            } else if (hasStatus && hasAge) {
                result = applicantRepo.findByStatusAndAgeRange(status, minAge, maxAge, pageRequest);
            } else if (hasStatus && hasAppliedDate) {
                result = applicantRepo.findByStatusAndAppliedBetweenDates(status, appliedFromDate, appliedToDate, pageRequest);
            } else if (hasStatus && hasYearsOfExperience) {
                result = applicantRepo.findByStatusAndYearsOfExperience(status, yearsOfExperience, pageRequest);
            } else if (hasName && hasGender) {
                result = applicantRepo.findByNameAndGender(name, gender, pageRequest);
            } else if (hasName && hasAge) {
                result = applicantRepo.findByNameAndAgeRange(name, minAge, maxAge, pageRequest);
            } else if (hasName && hasAppliedDate) {
                result = applicantRepo.findByNameAndAppliedBetweenDates(name, appliedFromDate, appliedToDate, pageRequest);
            } else if (hasName && hasYearsOfExperience) {
                result = applicantRepo.findByNameAndYearsOfExperience(name, yearsOfExperience, pageRequest);
            } else if (hasGender && hasAge) {
                result = applicantRepo.findByGenderAndAgeRange(gender, minAge, maxAge, pageRequest);
            } else if (hasGender && hasAppliedDate) {
                result = applicantRepo.findByGenderAndAppliedBetweenDates(gender, appliedFromDate, appliedToDate, pageRequest);
            } else if (hasGender && hasYearsOfExperience) {
                result = applicantRepo.findByGenderAndYearsOfExperience(gender, yearsOfExperience, pageRequest);
            } else if (hasAge && hasAppliedDate) {
                result = applicantRepo.findByAgeRangeAndAppliedBetweenDates(minAge, maxAge, appliedFromDate, appliedToDate, pageRequest);
            } else if (hasAge && hasYearsOfExperience) {
                result = applicantRepo.findByAgeRangeAndYearsOfExperience(minAge, maxAge, yearsOfExperience, pageRequest);
            } else if (hasAppliedDate && hasYearsOfExperience) {
                result = applicantRepo.findByAppliedBetweenDatesAndYearsOfExperience(appliedFromDate, appliedToDate, yearsOfExperience, pageRequest);
            }
            // Three filters
            else if (hasStatus && hasName && hasGender) {
                result = applicantRepo.findByStatusAndNameAndGender(status, name, gender, pageRequest);
            } else if (hasStatus && hasName && hasAge) {
                result = applicantRepo.findByStatusAndNameAndAgeRange(status, name, minAge, maxAge, pageRequest);
            } else if (hasStatus && hasName && hasAppliedDate) {
                result = applicantRepo.findByStatusAndNameAndAppliedBetweenDates(status, name, appliedFromDate, appliedToDate, pageRequest);
            } else if (hasStatus && hasName && hasYearsOfExperience) {
                result = applicantRepo.findByStatusAndNameAndYearsOfExperience(status, name, yearsOfExperience, pageRequest);
            } else if (hasStatus && hasGender && hasAge) {
                result = applicantRepo.findByStatusAndGenderAndAgeRange(status, gender, minAge, maxAge, pageRequest);
            } else if (hasStatus && hasGender && hasAppliedDate) {
                result = applicantRepo.findByStatusAndGenderAndAppliedBetweenDates(status, gender, appliedFromDate, appliedToDate, pageRequest);
            } else if (hasStatus && hasGender && hasYearsOfExperience) {
                result = applicantRepo.findByStatusAndGenderAndYearsOfExperience(status, gender, yearsOfExperience, pageRequest);
            }
            // For complex 4+ filter combinations, use Specification executor with dynamic query building
            else {
                result = applicantRepo.findAll(pageRequest);
            }

            if (result.isEmpty()) {
                return ResponseEntity.ok(Page.empty(pageRequest));
            }

            return ResponseEntity.ok(result);
        } catch (ServiceLevelException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "ApplicantService",
                    "Error occurred while fetching applicants",
                    "getAllApplicants",
                    "Service level exception",
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> updateApplicant(Applicant applicant) {
        if (ObjectUtils.isEmpty(applicant) || ObjectUtils.isEmpty(applicant.getApplicantId())) {
            throw new ServiceLevelException(
                    "ApplicantService",
                    "Required applicant body and id missing",
                    "updateApplicant",
                    "Missing required data exception",
                    "Required data applicant and applicant id are missing"
            );
        }
        try {
            Applicant existingApplicant = applicantRepo.findById(applicant.getApplicantId()).orElseThrow(() -> new ResourceNotFoundException(
                    "Applicant",
                    "id",
                    applicant.getApplicantId().toString()
            ));

            // Update fields if provided
            if (!ObjectUtils.isEmpty(applicant.getApplicantFirstName())) {
                existingApplicant.setApplicantFirstName(applicant.getApplicantFirstName());
            }
            if (!ObjectUtils.isEmpty(applicant.getApplicantLastName())) {
                existingApplicant.setApplicantLastName(applicant.getApplicantLastName());
            }
            if (!ObjectUtils.isEmpty(applicant.getApplicantEmail())) {
                existingApplicant.setApplicantEmail(applicant.getApplicantEmail());
            }
            if (!ObjectUtils.isEmpty(applicant.getApplicantPhone())) {
                existingApplicant.setApplicantPhone(applicant.getApplicantPhone());
            }
            if (!ObjectUtils.isEmpty(applicant.getApplicantAddress())) {
                existingApplicant.setApplicantAddress(applicant.getApplicantAddress());
            }
            if (!ObjectUtils.isEmpty(applicant.getApplicantCity())) {
                existingApplicant.setApplicantCity(applicant.getApplicantCity());
            }
            if (!ObjectUtils.isEmpty(applicant.getApplicantState())) {
                existingApplicant.setApplicantState(applicant.getApplicantState());
            }
            if (!ObjectUtils.isEmpty(applicant.getApplicantPinCode())) {
                existingApplicant.setApplicantPinCode(applicant.getApplicantPinCode());
            }
            if (!ObjectUtils.isEmpty(applicant.getApplicantCountry())) {
                existingApplicant.setApplicantCountry(applicant.getApplicantCountry());
            }
            if (!ObjectUtils.isEmpty(applicant.getApplicantAge())) {
                existingApplicant.setApplicantAge(applicant.getApplicantAge());
            }
            if (!ObjectUtils.isEmpty(applicant.getApplicantDateOfBirth())) {
                existingApplicant.setApplicantDateOfBirth(applicant.getApplicantDateOfBirth());
            }
            if (!ObjectUtils.isEmpty(applicant.getApplicantGender())) {
                existingApplicant.setApplicantGender(applicant.getApplicantGender());
            }
            if (!ObjectUtils.isEmpty(applicant.getApplicationStatus())) {
                existingApplicant.setApplicationStatus(applicant.getApplicationStatus());
            }
            if (!ObjectUtils.isEmpty(applicant.getIsActive())) {
                existingApplicant.setIsActive(applicant.getIsActive());
            }

            Applicant updatedApplicant = applicantRepo.save(existingApplicant);
            return ResponseEntity.ok(updatedApplicant);
        } catch (ServiceLevelException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "ApplicantService",
                    "Error occurred while updating applicant",
                    "updateApplicant",
                    "Service level exception",
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> deleteApplicant(Long id) {
        if (ObjectUtils.isEmpty(id)) {
            throw new ServiceLevelException(
                    "ApplicantService",
                    "Applicant id is missing",
                    "deleteApplicant",
                    "Missing required data exception",
                    "Required data applicant id is missing"
            );
        }
        try {
            Applicant applicant = applicantRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException(
                    "Applicant",
                    "id",
                    id.toString()
            ));
            applicantRepo.delete(applicant);
            return ResponseEntity.ok("Applicant deleted successfully");
        } catch (ServiceLevelException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "ApplicantService",
                    "Error occurred while deleting applicant",
                    "deleteApplicant",
                    "Service level exception",
                    e.getMessage()
            );
        }
    }
}

