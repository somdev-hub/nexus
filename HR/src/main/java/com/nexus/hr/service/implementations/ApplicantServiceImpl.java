package com.nexus.hr.service.implementations;

import com.nexus.hr.exception.ResourceNotFoundException;
import com.nexus.hr.exception.ServiceLevelException;
import com.nexus.hr.model.entities.*;
import com.nexus.hr.model.enums.ApplicationStatus;
import com.nexus.hr.payload.response.ApplicantTableResponse;
import com.nexus.hr.repository.ApplicantRepo;
import com.nexus.hr.repository.ApplicantRecruitmentMappingRepo;
import com.nexus.hr.repository.RecruitmentRepo;
import com.nexus.hr.service.interfaces.ApplicantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Service
@Slf4j
public class ApplicantServiceImpl implements ApplicantService {

	private final ApplicantRepo applicantRepo;
	private final AsyncDocumentService asyncDocumentService;
	private final ApplicantRecruitmentMappingRepo applicantRecruitmentMappingRepo;
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
					"Required data applicant is missing");
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
					e.getMessage());
		}
	}

	/**
	 * Create applicant with resume and cover letter document uploads
	 * Uploads documents to DMS concurrently and stores them via applicantDocuments
	 * relationship
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
					"Required data applicant is missing");
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
						"Required data recruitment id is missing");
			}
			Recruitment recruitment = recruitmentRepo.findById(recruitmentId)
					.orElseThrow(() -> new ResourceNotFoundException(
							"Recruitment",
							"id",
							recruitmentId.toString()));
			// applicant.setApplicationStatus(ApplicationStatus.APPLIED);
			// applicant.setRecruitment(recruitment);
			Applicant savedApplicant = applicantRepo.save(applicant);
			// recruitment.getApplicantsList().add(savedApplicant);
			recruitment.setTotalApplicants(
					recruitment.getTotalApplicants() != null ? recruitment.getTotalApplicants() + 1 : 1);
			recruitmentRepo.save(recruitment);
			log.info("Applicant saved with ID: {}", savedApplicant.getApplicantId());

			// Upload documents concurrently if provided
			if (!ObjectUtils.isEmpty(resume) || !ObjectUtils.isEmpty(coverLetter)) {
				log.info("Starting concurrent document upload for applicant: {}", savedApplicant.getApplicantId());

				CompletableFuture<AsyncDocumentService.DocumentResult> resumeFuture = ObjectUtils.isEmpty(resume)
						? CompletableFuture.completedFuture(
								new AsyncDocumentService.DocumentResult(null, null, "RESUME", true, null))
						: asyncDocumentService.uploadApplicantResume(resume, savedApplicant.getApplicantId());

				CompletableFuture<AsyncDocumentService.DocumentResult> coverLetterFuture = ObjectUtils
						.isEmpty(coverLetter)
								? CompletableFuture.completedFuture(
										new AsyncDocumentService.DocumentResult(null, null, "COVER_LETTER", true, null))
								: asyncDocumentService.uploadApplicantCoverLetter(coverLetter,
										savedApplicant.getApplicantId());

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
					e.getMessage());
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
					"Required data applicant id is missing");
		}
		try {
			Applicant applicant = applicantRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException(
					"Applicant",
					"id",
					id.toString()));
			return ResponseEntity.ok(applicant);
		} catch (Exception e) {
			throw new ServiceLevelException(
					"ApplicantService",
					"Error occurred while fetching applicant",
					"getApplicantById",
					"Service level exception",
					e.getMessage());
		}
	}

	@Override
	public ResponseEntity<?> getAllApplicants(
			Long recruitmentId,
			ApplicationStatus status,
			String name,
			Character gender,
			Integer minAge,
			Integer maxAge,
			LocalDate appliedFromDate,
			LocalDate appliedToDate,
			Double yearsOfExperience,
			Pageable pageRequest) {
		try {
			if (ObjectUtils.isEmpty(pageRequest)) {
				throw new ServiceLevelException(
						"ApplicantService",
						"PageRequest is required",
						"getAllApplicants",
						"Missing required data exception",
						"PageRequest cannot be null or empty");
			}
			Specification<Applicant> spec = buildApplicantSpecification(
					recruitmentId, status, name, gender, minAge, maxAge,
					appliedFromDate, appliedToDate, yearsOfExperience);
			Page<Applicant> result = applicantRepo.findAll(spec, pageRequest);
			if (result.isEmpty()) {
				return ResponseEntity.ok(Page.empty(pageRequest));
			}
			Page<ApplicantTableResponse> applicantTableResponses = result.map(applicant -> {
				ApplicantTableResponse applicantTableResponse = modelMapper.map(applicant,
						ApplicantTableResponse.class);
				Double totalYearsOfExperience = applicant.getApplicantExperiences().stream()
						.reduce(0.0,
								(sum, exp) -> sum
										+ (exp.getYearsOfExperience() != null ? exp.getYearsOfExperience() : 0.0),
								Double::sum);
				applicantTableResponse.setTotalYearsOfExperience(totalYearsOfExperience);
				applicantTableResponse.setPreviousCompany(
						applicant.getApplicantExperiences().stream()
								.sorted((e1, e2) -> {
									if (e1.getEndDate() == null && e2.getEndDate() == null)
										return 0;
									if (e1.getEndDate() == null)
										return -1;
									if (e2.getEndDate() == null)
										return 1;
									return e2.getEndDate().compareTo(e1.getEndDate());
								})
								.map(ApplicantExperience::getPreviousCompany)
								.findFirst()
								.orElse(null));
				// Set applicantRecruitmentMappingId from the recruitment mapping
				applicant.getApplicantRecruitmentMappings().stream()
						.filter(mapping -> mapping.getRecruitment() != null && 
								mapping.getRecruitment().getRecruitmentId().equals(recruitmentId))
						.findFirst()
						.ifPresent(mapping -> applicantTableResponse.setApplicantRecruitmentMappingId(mapping.getApplicantRecruitmentMappingId()));
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
					e.getMessage());
		}
	}

	private Specification<Applicant> buildApplicantSpecification(
			Long recruitmentId,
			ApplicationStatus status,
			String name,
			Character gender,
			Integer minAge,
			Integer maxAge,
			LocalDate appliedFromDate,
			LocalDate appliedToDate,
			Double yearsOfExperience) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			Join<Applicant, ApplicantRecruitmentMapping> mappingJoin = root.join("applicantRecruitmentMappings",
					JoinType.INNER);
			predicates.add(cb.equal(mappingJoin.get("recruitment").get("recruitmentId"), recruitmentId));
			if (!ObjectUtils.isEmpty(status)) {
				predicates.add(cb.equal(mappingJoin.get("status"), status));
			}
			if (!ObjectUtils.isEmpty(name)) {
				String namePattern = "%" + name.toLowerCase() + "%";
				Predicate firstNameMatch = cb.like(cb.lower(root.get("applicantFirstName")), namePattern);
				Predicate lastNameMatch = cb.like(cb.lower(root.get("applicantLastName")), namePattern);
				predicates.add(cb.or(firstNameMatch, lastNameMatch));
			}
			if (!ObjectUtils.isEmpty(gender)) {
				predicates.add(cb.equal(root.get("applicantGender"), gender));
			}
			if (!ObjectUtils.isEmpty(minAge) && !ObjectUtils.isEmpty(maxAge)) {
				predicates.add(cb.between(root.get("applicantAge"), minAge, maxAge));
			}
			if (!ObjectUtils.isEmpty(appliedFromDate) && !ObjectUtils.isEmpty(appliedToDate)) {
				predicates.add(cb.between(cb.function("DATE", LocalDate.class, mappingJoin.get("appliedOn")),
						appliedFromDate, appliedToDate));
			}
			if (!ObjectUtils.isEmpty(yearsOfExperience)) {
				Join<Applicant, ApplicantExperience> experienceJoin = root.join("applicantExperiences", JoinType.INNER);
				predicates.add(cb.ge(experienceJoin.get("yearsOfExperience"), yearsOfExperience));
				query.distinct(true);
			}
			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	@Override
	public ResponseEntity<?> updateApplicant(Applicant applicant, Long userId) {
		if (ObjectUtils.isEmpty(applicant) || ObjectUtils.isEmpty(applicant.getApplicantId())) {
			throw new ServiceLevelException(
					"ApplicantService",
					"Required applicant body and id missing",
					"updateApplicant",
					"Missing required data exception",
					"Required data applicant and applicant id are missing");
		}
		try {
			Applicant existingApplicant;
			if (!ObjectUtils.isEmpty(userId)) {
				existingApplicant = applicantRepo.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException(
						"Applicant",
						"userId",
						userId.toString()));
			} else {
				existingApplicant = applicantRepo.findById(applicant.getApplicantId())
						.orElseThrow(() -> new ResourceNotFoundException(
								"Applicant",
								"id",
								applicant.getApplicantId().toString()));
			}

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
			// if (!ObjectUtils.isEmpty(applicant.getApplicationStatus())) {
			// existingApplicant.setApplicationStatus(applicant.getApplicationStatus());
			// }
			if (!ObjectUtils.isEmpty(applicant.getIsActive())) {
				existingApplicant.setIsActive(applicant.getIsActive());
			}

			if (!ObjectUtils.isEmpty(applicant.getApplicantEducations())) {
				// Merge into existing collection to avoid orphanRemoval issues:
				// replacing the entire collection reference causes Hibernate to
				// detect the old collection as orphaned, triggering deletion conflicts.
				existingApplicant.getApplicantEducations().clear();
				applicant.getApplicantEducations().forEach(edu -> edu.setApplicant(existingApplicant));
				existingApplicant.getApplicantEducations().addAll(applicant.getApplicantEducations());
			}
			if (!ObjectUtils.isEmpty(applicant.getApplicantExperiences())) {
				existingApplicant.getApplicantExperiences().clear();
				applicant.getApplicantExperiences().forEach(exp -> exp.setApplicant(existingApplicant));
				existingApplicant.getApplicantExperiences().addAll(applicant.getApplicantExperiences());
			}
			if (!ObjectUtils.isEmpty(applicant.getApplicantSkills())) {
				existingApplicant.getApplicantSkills().clear();
				applicant.getApplicantSkills().forEach(skill -> skill.setApplicant(existingApplicant));
				existingApplicant.getApplicantSkills().addAll(applicant.getApplicantSkills());
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
					e.getMessage());
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
					"Required data applicant id is missing");
		}
		try {
			Applicant applicant = applicantRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException(
					"Applicant",
					"id",
					id.toString()));
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
					e.getMessage());
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
					"Required data applicant is missing");
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
					e.getMessage());
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
					"Required data applicant education or user id is missing");
		}
		try {
			Applicant applicant = applicantRepo.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException(
					"Applicant",
					"userId",
					userId.toString()));
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
					e.getMessage());
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
					"Required data applicant experience or user id is missing");
		}
		try {
			Applicant applicant = applicantRepo.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException(
					"Applicant",
					"userId",
					userId.toString()));
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
					e.getMessage());
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
					"Required data applicant skill or user id is missing");
		}
		try {
			Applicant applicant = applicantRepo.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException(
					"Applicant",
					"userId",
					userId.toString()));
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
					e.getMessage());
		}
	}

	@Override
	public ResponseEntity<?> getApplicantByUserId(Long userId) {
		if (ObjectUtils.isEmpty(userId)) {
			throw new ServiceLevelException(
					"ApplicantService",
					"Required user id missing",
					"getApplicantByUserId",
					"Missing required data exception",
					"Required data user id is missing");
		}
		try {
			Applicant applicant = applicantRepo.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException(
					"Applicant",
					"userId",
					userId.toString()));
			applicant.setApplicantEducations(applicantRepo
					.findApplicantEducationByApplicant_ApplicantIdAndIsActiveTrue(applicant.getApplicantId()));
			applicant.setApplicantExperiences(applicantRepo
					.findApplicantExperienceByApplicant_ApplicantIdAndIsActiveTrue(applicant.getApplicantId()));
			applicant.setApplicantSkills(
					applicantRepo.findApplicantSkillByApplicant_ApplicantIdAndIsActiveTrue(applicant.getApplicantId()));
			applicant.setApplicantDocuments(applicantRepo
					.findApplicantDocumentsByApplicant_ApplicantIdAndIsActiveTrue(applicant.getApplicantId()));
			return ResponseEntity.ok(applicant);
		} catch (ServiceLevelException e) {
			throw e;
		} catch (Exception e) {
			throw new ServiceLevelException(
					"ApplicantService",
					"Error occurred while fetching applicant by user id",
					"getApplicantByUserId",
					"Service level exception",
					e.getMessage());
		}
	}

	@Override
	public ResponseEntity<?> addApplicantDocument(MultipartFile document, Long userId) {
		if (ObjectUtils.isEmpty(document) || ObjectUtils.isEmpty(userId)) {
			throw new ServiceLevelException(
					"ApplicantService",
					"Required document or user id missing",
					"addApplicantDocument",
					"Missing required data exception",
					"Required data document or user id is missing");
		}
		try {
			Applicant applicant = applicantRepo.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException(
					"Applicant",
					"userId",
					userId.toString()));
			ResponseEntity<?> response = asyncDocumentService.callDmsToUpload(document, userId,
					"Resume_Applicant_" + applicant.getApplicantId() + ".pdf", "RESUME", applicant.getApplicantId());
			if (response.getStatusCode().is2xxSuccessful()) {
				@SuppressWarnings("unchecked")
				Map<String, String> responseBody = (Map<String, String>) response.getBody();
				if (responseBody != null && responseBody.containsKey("documentUrl")) {
					HrDocument hrDocument = new HrDocument();
					hrDocument.setApplicant(applicant);
					hrDocument.setDocumentUrl(responseBody.get("documentUrl"));
					hrDocument.setDocumentName(responseBody.get("documentName"));
					hrDocument.setHrDocumentType(responseBody.get("documentType"));

					applicant.getApplicantDocuments().add(hrDocument);
					applicantRepo.save(applicant);
					return ResponseEntity.ok("Document uploaded and saved successfully");
				} else {
					log.error("Error uploading Resume to DMS: {}",
							responseBody != null ? responseBody.get("errorMessage") : "Unknown error");
					throw new ServiceLevelException(
							"ApplicantService",
							"Error occurred while uploading document to DMS",
							"addApplicantDocument",
							"Service level exception",
							responseBody != null ? responseBody.get("errorMessage") : "Unknown error");
				}
			} else {
				log.error("Error uploading Resume to DMS: {}", response.getStatusCode());
				throw new ServiceLevelException(
						"ApplicantService",
						"Error occurred while uploading document to DMS",
						"addApplicantDocument",
						"Service level exception",
						"DMS service returned status: " + response.getStatusCode());
			}
		} catch (ServiceLevelException e) {
			throw e;
		} catch (Exception e) {
			throw new ServiceLevelException(
					"ApplicantService",
					"Error occurred while adding applicant document",
					"addApplicantDocument",
					"Service level exception",
					e.getMessage());
		}
	}

	@Override
	@Transactional
	public ResponseEntity<?> deleteApplicantDocument(Long userId, Long hrDocumentId) {
		if (ObjectUtils.isEmpty(userId) || ObjectUtils.isEmpty(hrDocumentId)) {
			throw new ServiceLevelException(
					"ApplicantService",
					"Required user id or document id missing",
					"deleteApplicantDocument",
					"Missing required data exception",
					"Required data user id or document id is missing");
		}
		try {
			Applicant applicant = applicantRepo.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException(
					"Applicant",
					"userId",
					userId.toString()));
			// find and set isActive to false
			HrDocument hrDocument = applicant.getApplicantDocuments().stream()
					.filter(doc -> doc.getHrDocumentId().equals(hrDocumentId))
					.findFirst()
					.orElseThrow(() -> new ResourceNotFoundException(
							"HrDocument",
							"hrDocumentId",
							hrDocumentId.toString()));
			hrDocument.setIsActive(false);
			applicantRepo.save(applicant);
			return ResponseEntity.ok("Document deleted successfully");
		} catch (ServiceLevelException e) {
			throw e;
		} catch (Exception e) {
			throw new ServiceLevelException(
					"ApplicantService",
					"Error occurred while deleting applicant document",
					"deleteApplicantDocument",
					"Service level exception",
					e.getMessage());
		}
	}

    @Override
    public ResponseEntity<?> getApplicantByRecruitmentMapping(Long applicantRecruitmentMappingId) {
        if (ObjectUtils.isEmpty(applicantRecruitmentMappingId)) {
            throw new ServiceLevelException(
                    "ApplicantService",
                    "Applicant recruitment mapping id is missing",
                    "getApplicantByRecruitmentMapping",
                    "Missing required data exception",
                    "Required data applicant recruitment mapping id is missing");
        }
        try {
            ApplicantRecruitmentMapping mapping = applicantRecruitmentMappingRepo.findById(applicantRecruitmentMappingId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "ApplicantRecruitmentMapping",
                            "id",
                            applicantRecruitmentMappingId.toString()));

            Applicant applicant = mapping.getApplicant();
            // The @SQLRestriction on Applicant collections ensures only active data is fetched
            // The applicationDocuments in the mapping are already filtered by isActive via the relationship

            return ResponseEntity.ok(applicant);
        } catch (ServiceLevelException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "ApplicantService",
                    "Error occurred while fetching applicant by recruitment mapping",
                    "getApplicantByRecruitmentMapping",
                    "Service level exception",
                    e.getMessage());
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateApplicantRecruitmentStatus(Long applicantRecruitmentMappingId, ApplicationStatus status) {
        if (ObjectUtils.isEmpty(applicantRecruitmentMappingId) || ObjectUtils.isEmpty(status)) {
            throw new ServiceLevelException(
                    "ApplicantService",
                    "Applicant recruitment mapping id or status is missing",
                    "updateApplicantRecruitmentStatus",
                    "Missing required data exception",
                    "Required data applicant recruitment mapping id or status is missing");
        }
        try {
            ApplicantRecruitmentMapping mapping = applicantRecruitmentMappingRepo.findById(applicantRecruitmentMappingId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "ApplicantRecruitmentMapping",
                            "id",
                            applicantRecruitmentMappingId.toString()));

            // Create status history entry
            ApplicantRecruitmentMappingStatusHist statusHist = new ApplicantRecruitmentMappingStatusHist();
            statusHist.setStatus(status);
            statusHist.setApplicantRecruitmentMapping(mapping);
            statusHist.setIsActive(true);
            mapping.getStatusHistory().add(statusHist);

            // Update the current status
            mapping.setStatus(status);
            applicantRecruitmentMappingRepo.save(mapping);

            return ResponseEntity.ok("Applicant recruitment status updated successfully to " + status);
        } catch (ServiceLevelException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "ApplicantService",
                    "Error occurred while updating applicant recruitment status",
                    "updateApplicantRecruitmentStatus",
                    "Service level exception",
                    e.getMessage());
        }
    }
}