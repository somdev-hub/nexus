package com.nexus.hr.service.implementations;

import com.nexus.hr.exception.ResourceNotFoundException;
import com.nexus.hr.exception.ServiceLevelException;
import com.nexus.hr.model.entities.*;
import com.nexus.hr.model.enums.ApplicationStatus;
import com.nexus.hr.payload.response.ApplicantTableResponse;
import com.nexus.hr.repository.ApplicantRepo;
import com.nexus.hr.repository.RecruitmentRepo;
import com.nexus.hr.service.interfaces.ApplicantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Service
@Slf4j
public class ApplicantServiceImpl implements ApplicantService {

    private final ApplicantRepo applicantRepo;
    private final AsyncDocumentService asyncDocumentService;
    private final RecruitmentRepo recruitmentRepo;
    private final ModelMapper modelMapper;

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

    /**
     * Create applicant with resume and cover letter document uploads
     * Uploads documents to DMS concurrently and stores them via applicantDocuments relationship
     */
    @Override
    @Transactional
    public ResponseEntity<?> createApplicantWithDocuments(
            Long recruitmentId, Applicant applicant,
            MultipartFile resume,
            MultipartFile coverLetter) {
        if (ObjectUtils.isEmpty(applicant)) {
            throw new ServiceLevelException(
                    "ApplicantService",
                    "Required applicant body missing",
                    "createApplicantWithDocuments",
                    "Missing required data exception",
                    "Required data applicant is missing"
            );
        }
        try {
            // Save applicant first to get applicant ID
            log.info("Saving applicant: {} {}", applicant.getApplicantFirstName(), applicant.getApplicantLastName());
            if (ObjectUtils.isEmpty(recruitmentId)) {
                throw new ServiceLevelException(
                        "ApplicantService",
                        "Recruitment id is missing",
                        "createApplicantWithDocuments",
                        "Missing required data exception",
                        "Required data recruitment id is missing"
                );
            }
            Recruitment recruitment = recruitmentRepo.findById(recruitmentId).orElseThrow(() -> new ResourceNotFoundException(
                    "Recruitment",
                    "id",
                    recruitmentId.toString()
            ));
//            applicant.setApplicationStatus(ApplicationStatus.APPLIED);
//            applicant.setRecruitment(recruitment);
            Applicant savedApplicant = applicantRepo.save(applicant);
//            recruitment.getApplicantsList().add(savedApplicant);
            recruitment.setTotalApplicants(recruitment.getTotalApplicants() != null ? recruitment.getTotalApplicants() + 1 : 1);
            recruitmentRepo.save(recruitment);
            log.info("Applicant saved with ID: {}", savedApplicant.getApplicantId());

            // Upload documents concurrently if provided
            if (!ObjectUtils.isEmpty(resume) || !ObjectUtils.isEmpty(coverLetter)) {
                log.info("Starting concurrent document upload for applicant: {}", savedApplicant.getApplicantId());

                CompletableFuture<AsyncDocumentService.DocumentResult> resumeFuture = ObjectUtils.isEmpty(resume)
                        ? CompletableFuture.completedFuture(new AsyncDocumentService.DocumentResult(null, null, "RESUME", true, null))
                        : asyncDocumentService.uploadApplicantResume(resume, savedApplicant.getApplicantId());

                CompletableFuture<AsyncDocumentService.DocumentResult> coverLetterFuture = ObjectUtils.isEmpty(coverLetter)
                        ? CompletableFuture.completedFuture(new AsyncDocumentService.DocumentResult(null, null, "COVER_LETTER", true, null))
                        : asyncDocumentService.uploadApplicantCoverLetter(coverLetter, savedApplicant.getApplicantId());

                // Wait for all document uploads to complete
                CompletableFuture.allOf(resumeFuture, coverLetterFuture).join();
                log.info("Document upload tasks completed for applicant: {}", savedApplicant.getApplicantId());

                // Get results
                AsyncDocumentService.DocumentResult resumeResult = resumeFuture.join();
                AsyncDocumentService.DocumentResult coverLetterResult = coverLetterFuture.join();

                // Create HrDocument objects from upload results and add to applicantDocuments
                if (resumeResult.isSuccess() && !ObjectUtils.isEmpty(resumeResult.getDocumentUrl())) {
                    HrDocument resumeDoc = new HrDocument();
                    resumeDoc.setDocumentName(resumeResult.getDocumentName());
                    resumeDoc.setDocumentUrl(resumeResult.getDocumentUrl());
                    resumeDoc.setHrDocumentType(resumeResult.getDocumentType());
                    resumeDoc.setApplicant(savedApplicant);
                    savedApplicant.getApplicantDocuments().add(resumeDoc);
                    log.info("Resume document added for applicant: {}", savedApplicant.getApplicantId());
                } else if (!ObjectUtils.isEmpty(resume)) {
                    log.error("Error uploading Resume to DMS: {}", resumeResult.getErrorMessage());
                }

                if (coverLetterResult.isSuccess() && !ObjectUtils.isEmpty(coverLetterResult.getDocumentUrl())) {
                    HrDocument coverLetterDoc = new HrDocument();
                    coverLetterDoc.setDocumentName(coverLetterResult.getDocumentName());
                    coverLetterDoc.setDocumentUrl(coverLetterResult.getDocumentUrl());
                    coverLetterDoc.setHrDocumentType(coverLetterResult.getDocumentType());
                    coverLetterDoc.setApplicant(savedApplicant);
                    savedApplicant.getApplicantDocuments().add(coverLetterDoc);
                    log.info("Cover Letter document added for applicant: {}", savedApplicant.getApplicantId());
                } else if (!ObjectUtils.isEmpty(coverLetter)) {
                    log.error("Error uploading Cover Letter to DMS: {}", coverLetterResult.getErrorMessage());
                }

                // Update applicant with documents
                savedApplicant = applicantRepo.save(savedApplicant);
                log.info("Applicant updated with documents");
            }

            return ResponseEntity.ok(savedApplicant);
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "ApplicantService",
                    "Error occurred while creating applicant with documents",
                    "createApplicantWithDocuments",
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
            Long recruitmentId, ApplicationStatus status,
            String name,
            Character gender,
            Integer minAge,
            Integer maxAge,
            LocalDate appliedFromDate,
            LocalDate appliedToDate,
            Double yearsOfExperience,
            Pageable pageRequest
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

            // No filters - return all (filter by recruitmentId only)
            if (!hasStatus && !hasName && !hasGender && !hasAge && !hasAppliedDate && !hasYearsOfExperience) {
                result = applicantRepo.findByRecruitment_RecruitmentId(recruitmentId, pageRequest);
            }
            // Only one filter
            else if (hasStatus && !hasName && !hasGender && !hasAge && !hasAppliedDate && !hasYearsOfExperience) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndApplicationStatus(recruitmentId, status, pageRequest);
            } else if (hasName && !hasStatus && !hasGender && !hasAge && !hasAppliedDate && !hasYearsOfExperience) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndNameContaining(recruitmentId, name, pageRequest);
            } else if (hasGender && !hasStatus && !hasName && !hasAge && !hasAppliedDate && !hasYearsOfExperience) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndApplicantGender(recruitmentId, gender, pageRequest);
            } else if (hasAge && !hasStatus && !hasName && !hasGender && !hasAppliedDate && !hasYearsOfExperience) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndAgeRange(recruitmentId, minAge, maxAge, pageRequest);
            } else if (hasAppliedDate && !hasStatus && !hasName && !hasGender && !hasAge && !hasYearsOfExperience) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndAppliedBetweenDates(recruitmentId, appliedFromDate, appliedToDate, pageRequest);
            } else if (hasYearsOfExperience && !hasStatus && !hasName && !hasGender && !hasAge && !hasAppliedDate) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndYearsOfExperience(recruitmentId, yearsOfExperience, pageRequest);
            }
            // Two filters
            else if (hasStatus && hasName) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndStatusAndName(recruitmentId, status, name, pageRequest);
            } else if (hasStatus && hasGender) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndStatusAndGender(recruitmentId, status, gender, pageRequest);
            } else if (hasStatus && hasAge) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndStatusAndAgeRange(recruitmentId, status, minAge, maxAge, pageRequest);
            } else if (hasStatus && hasAppliedDate) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndStatusAndAppliedBetweenDates(recruitmentId, status, appliedFromDate, appliedToDate, pageRequest);
            } else if (hasStatus && hasYearsOfExperience) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndStatusAndYearsOfExperience(recruitmentId, status, yearsOfExperience, pageRequest);
            } else if (hasName && hasGender) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndNameAndGender(recruitmentId, name, gender, pageRequest);
            } else if (hasName && hasAge) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndNameAndAgeRange(recruitmentId, name, minAge, maxAge, pageRequest);
            } else if (hasName && hasAppliedDate) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndNameAndAppliedBetweenDates(recruitmentId, name, appliedFromDate, appliedToDate, pageRequest);
            } else if (hasName && hasYearsOfExperience) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndNameAndYearsOfExperience(recruitmentId, name, yearsOfExperience, pageRequest);
            } else if (hasGender && hasAge) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndGenderAndAgeRange(recruitmentId, gender, minAge, maxAge, pageRequest);
            } else if (hasGender && hasAppliedDate) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndGenderAndAppliedBetweenDates(recruitmentId, gender, appliedFromDate, appliedToDate, pageRequest);
            } else if (hasGender && hasYearsOfExperience) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndGenderAndYearsOfExperience(recruitmentId, gender, yearsOfExperience, pageRequest);
            } else if (hasAge && hasAppliedDate) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndAgeRangeAndAppliedBetweenDates(recruitmentId, minAge, maxAge, appliedFromDate, appliedToDate, pageRequest);
            } else if (hasAge && hasYearsOfExperience) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndAgeRangeAndYearsOfExperience(recruitmentId, minAge, maxAge, yearsOfExperience, pageRequest);
            } else if (hasAppliedDate && hasYearsOfExperience) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndAppliedBetweenDatesAndYearsOfExperience(recruitmentId, appliedFromDate, appliedToDate, yearsOfExperience, pageRequest);
            }
            // Three filters
            else if (hasStatus && hasName && hasGender) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndStatusAndNameAndGender(recruitmentId, status, name, gender, pageRequest);
            } else if (hasStatus && hasName && hasAge) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndStatusAndNameAndAgeRange(recruitmentId, status, name, minAge, maxAge, pageRequest);
            } else if (hasStatus && hasName && hasAppliedDate) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndStatusAndNameAndAppliedBetweenDates(recruitmentId, status, name, appliedFromDate, appliedToDate, pageRequest);
            } else if (hasStatus && hasName && hasYearsOfExperience) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndStatusAndNameAndYearsOfExperience(recruitmentId, status, name, yearsOfExperience, pageRequest);
            } else if (hasStatus && hasGender && hasAge) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndStatusAndGenderAndAgeRange(recruitmentId, status, gender, minAge, maxAge, pageRequest);
            } else if (hasStatus && hasGender && hasAppliedDate) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndStatusAndGenderAndAppliedBetweenDates(recruitmentId, status, gender, appliedFromDate, appliedToDate, pageRequest);
            } else if (hasStatus && hasGender && hasYearsOfExperience) {
                result = applicantRepo.findByRecruitment_RecruitmentIdAndStatusAndGenderAndYearsOfExperience(recruitmentId, status, gender, yearsOfExperience, pageRequest);
            }
            // For complex 4+ filter combinations, use Specification executor with dynamic query building
            else {
                result = applicantRepo.findByRecruitment_RecruitmentId(recruitmentId, pageRequest);
            }

            if (result.isEmpty()) {
                return ResponseEntity.ok(Page.empty(pageRequest));
            }

            Page<ApplicantTableResponse> applicantTableResponses = result.map(applicant -> {
                ApplicantTableResponse applicantTableResponse = modelMapper.map(applicant, ApplicantTableResponse.class);
                Double totalYearsOfExperience = applicant.getApplicantExperiences().stream().reduce(0.0, (sum, exp) -> sum + (exp.getYearsOfExperience() != null ? exp.getYearsOfExperience() : 0.0), Double::sum);
                applicantTableResponse.setTotalYearsOfExperience(totalYearsOfExperience);
                // get previous company name sorted by endDate
                applicantTableResponse.setPreviousCompany(
                        applicant.getApplicantExperiences().stream()
                                .sorted((e1, e2) -> {
                                    if (e1.getEndDate() == null && e2.getEndDate() == null) return 0;
                                    if (e1.getEndDate() == null) return -1;
                                    if (e2.getEndDate() == null) return 1;
                                    return e2.getEndDate().compareTo(e1.getEndDate());
                                })
                                .map(ApplicantExperience::getPreviousCompany)
                                .findFirst()
                                .orElse(null)
                );
                return applicantTableResponse;
            });

            return ResponseEntity.ok(applicantTableResponses);
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
//            if (!ObjectUtils.isEmpty(applicant.getApplicationStatus())) {
//                existingApplicant.setApplicationStatus(applicant.getApplicationStatus());
//            }
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

    @Override
    public ResponseEntity<?> createApplicantWithoutDocuments(Applicant applicant) {
        if (ObjectUtils.isEmpty(applicant)) {
            throw new ServiceLevelException(
                    "ApplicantService",
                    "Required applicant body missing",
                    "createApplicantWithoutDocuments",
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
                    "Error occurred while creating applicant without documents",
                    "createApplicantWithoutDocuments",
                    "Service level exception",
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> createApplicantEducation(ApplicantEducation applicantEducation, Long userId) {
        if (ObjectUtils.isEmpty(applicantEducation) || ObjectUtils.isEmpty(userId)) {
            throw new ServiceLevelException(
                    "ApplicantService",
                    "Required applicant education body or user id missing",
                    "createApplicantEducation",
                    "Missing required data exception",
                    "Required data applicant education or user id is missing"
            );
        }
        try {
            Applicant applicant = applicantRepo.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException(
                    "Applicant",
                    "userId",
                    userId.toString()
            ));
            applicantEducation.setApplicant(applicant);
            applicant.getApplicantEducations().add(applicantEducation);
            Applicant updatedApplicant = applicantRepo.save(applicant);
            return ResponseEntity.ok(updatedApplicant);
        } catch (ServiceLevelException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "ApplicantService",
                    "Error occurred while creating applicant education",
                    "createApplicantEducation",
                    "Service level exception",
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> createApplicantExperience(ApplicantExperience applicantExperience, Long userId) {
        if (ObjectUtils.isEmpty(applicantExperience) || ObjectUtils.isEmpty(userId)) {
            throw new ServiceLevelException(
                    "ApplicantService",
                    "Required applicant experience body or user id missing",
                    "createApplicantExperience",
                    "Missing required data exception",
                    "Required data applicant experience or user id is missing"
            );
        }
        try {
            Applicant applicant = applicantRepo.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException(
                    "Applicant",
                    "userId",
                    userId.toString()
            ));
            applicantExperience.setApplicant(applicant);
            applicant.getApplicantExperiences().add(applicantExperience);
            Applicant updatedApplicant = applicantRepo.save(applicant);
            return ResponseEntity.ok(updatedApplicant);
        } catch (ServiceLevelException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "ApplicantService",
                    "Error occurred while creating applicant experience",
                    "createApplicantExperience",
                    "Service level exception",
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> createApplicantSkill(ApplicantSkill applicantSkill, Long userId) {
        if (ObjectUtils.isEmpty(applicantSkill) || ObjectUtils.isEmpty(userId)) {
            throw new ServiceLevelException(
                    "ApplicantService",
                    "Required applicant skill body or user id missing",
                    "createApplicantSkill",
                    "Missing required data exception",
                    "Required data applicant skill or user id is missing"
            );
        }
        try {
            Applicant applicant = applicantRepo.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException(
                    "Applicant",
                    "userId",
                    userId.toString()
            ));
            applicantSkill.setApplicant(applicant);
            applicant.getApplicantSkills().add(applicantSkill);
            Applicant updatedApplicant = applicantRepo.save(applicant);
            return ResponseEntity.ok(updatedApplicant);
        } catch (ServiceLevelException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "ApplicantService",
                    "Error occurred while creating applicant skill",
                    "createApplicantSkill",
                    "Service level exception",
                    e.getMessage()
            );
        }
    }
}

