package com.nexus.iam.controller;

import com.nexus.iam.service.RecruitmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

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
	public ResponseEntity<?> getAllRecruitments(@RequestParam Long orgId,
			@RequestParam(required = false) Boolean isActive, @RequestParam(required = false) Long empId,
			@RequestParam(required = false) String hiringType, @RequestParam(required = false) String hiringStatus,
			@RequestParam(required = false, defaultValue = "0") Integer pageNo,
			@RequestParam(required = false, defaultValue = "10") Integer pageOffset) {
		return recruitmentService.getAllRecruitments(orgId, isActive, empId, hiringType, hiringStatus, pageNo,
				pageOffset);
	}

	@PutMapping("/")
	public ResponseEntity<?> updateRecruitment(@RequestBody String recruitment, @RequestParam Long empId) {
		return recruitmentService.updateRecruitment(recruitment, empId);
	}

	@GetMapping("/closed")
	public ResponseEntity<?> closeRecruitment(@RequestParam Long orgId,
			@RequestParam(required = false, defaultValue = "0") Integer pageNo,
			@RequestParam(required = false, defaultValue = "10") Integer pageOffset) {
		return recruitmentService.getClosedRecruitments(orgId, pageNo, pageOffset);
	}

	@PostMapping(value = "/applicant", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> createApplicantWithDocuments(
			@RequestPart("recruitmentId") Long recruitmentId,
			@RequestPart("applicant") String applicant,
			@RequestPart(value = "resume", required = false) MultipartFile resume,
			@RequestPart(value = "coverLetter", required = false) MultipartFile coverLetter) {
		return recruitmentService.createApplicantWithDocuments(recruitmentId, applicant, resume, coverLetter);
	}

	@GetMapping("/applicant")
	public ResponseEntity<?> getAllApplicants(
			@RequestParam Long recruitmentId,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String name,
			@RequestParam(required = false) Character gender,
			@RequestParam(required = false) Integer minAge,
			@RequestParam(required = false) Integer maxAge,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appliedFromDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appliedToDate,
			@RequestParam(required = false) Integer yearsOfExperience,
			@RequestParam(required = false, defaultValue = "0") Integer pageNo,
			@RequestParam(required = false, defaultValue = "10") Integer pageSize) {
		return recruitmentService.getAllApplicants(
				recruitmentId,
				status,
				name,
				gender,
				minAge,
				maxAge,
				appliedFromDate,
				appliedToDate,
				yearsOfExperience,
				pageNo,
				pageSize);
	}

	@GetMapping("/applicant/applicantId/{id}")
	public ResponseEntity<?> getApplicantByApplicantId(@PathVariable Long id) {
		return recruitmentService.getApplicantByApplicantId(id);
	}

	/**
	 * Get applicant by recruitment mapping using applicant ID and recruitment ID
	 * Returns applicant with active collections and only the application documents
	 * associated with the specific recruitment mapping
	 *
	 * @param applicantId   Applicant ID
	 * @param recruitmentId Recruitment ID
	 * @return Applicant details with recruitment-scoped documents
	 */
	@GetMapping("/applicant/recruitment-mapping")
	public ResponseEntity<?> getApplicantByRecruitmentMapping(
			@RequestParam Long applicantId,
			@RequestParam Long recruitmentId) {
		return recruitmentService.getApplicantByRecruitmentMapping(applicantId, recruitmentId);
	}

	/**
	 * Update applicant recruitment status (Schedule interview, accept, reject,
	 * etc.)
	 *
	 * @param applicantId   Applicant ID
	 * @param recruitmentId Recruitment ID
	 * @param request       Status update request containing status and optional
	 *                      interview details
	 * @return Success message
	 */
	@PutMapping("/applicant/recruitment-mapping/status")
	public ResponseEntity<?> updateApplicantRecruitmentStatus(
			@RequestParam Long applicantId,
			@RequestParam Long recruitmentId,
			@RequestBody String request) {
		return recruitmentService.updateApplicantRecruitmentStatus(applicantId, recruitmentId, request);
	}

	/**
	 * Get applicant by ID
	 *
	 * @param id Applicant ID
	 * @return Applicant details
	 */
	@GetMapping("/applicant/{id}")
	public ResponseEntity<?> getApplicantById(@PathVariable Long id) {
		return recruitmentService.getApplicantById(id);
	}

	@GetMapping("/analytics")
	public ResponseEntity<?> getRecruitmentAnalytics(@RequestParam Long orgId) {
		return recruitmentService.getRecruitmentAnalytics(orgId);
	}

	@PostMapping("/applicant/education")
	public ResponseEntity<?> addApplicantEducation(@RequestBody String education, @RequestParam Long userId) {
		return recruitmentService.addApplicantEducation(education, userId);
	}

	@PostMapping("/applicant/experience")
	public ResponseEntity<?> addApplicantExperience(@RequestBody String experience, @RequestParam Long userId) {
		return recruitmentService.addApplicantExperience(experience, userId);
	}

	@PostMapping("/applicant/skill")
	public ResponseEntity<?> addApplicantSkill(@RequestBody String skill, @RequestParam Long userId) {
		return recruitmentService.addApplicantSkill(skill, userId);
	}

	@PutMapping("/applicant")
	public ResponseEntity<?> updateApplicant(@RequestBody String payload, @RequestParam Long userId) {
		return recruitmentService.updateApplicant(payload, userId);
	}

	@GetMapping("/openings-today")
	public ResponseEntity<?> getOpeningsToday(@RequestParam(required = false, defaultValue = "0") Integer pageNo,
			@RequestParam(required = false, defaultValue = "10") Integer pageOffset,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String orgName,
			@RequestParam(required = false) String location,
			@RequestParam(required = false) String query) {
		return recruitmentService.getOpeningsToday(pageNo, pageOffset, status, orgName, location, query);
	}

	@GetMapping("/openings-before-today")
	public ResponseEntity<?> getOpeningsBeforeToday(@RequestParam(required = false, defaultValue = "0") Integer pageNo,
			@RequestParam(required = false, defaultValue = "10") Integer pageOffset,
			@RequestParam(required = false) String status, @RequestParam(required = false) String orgName,
			@RequestParam(required = false) String location, @RequestParam(required = false) String query) {
		return recruitmentService.getOpeningsBeforeToday(pageNo, pageOffset, status, orgName, location, query);
	}

	@GetMapping("/position-pie-graph")
	public ResponseEntity<?> getPositionPieGraph() {
		return recruitmentService.getPositionPieGraph();
	}

	@GetMapping("/openings-experience-wise")
	public ResponseEntity<?> getExperienceWiseOpenings() {
		return recruitmentService.getExperienceWiseOpenings();
	}

	@GetMapping("/company-wise-opening-count")
	public ResponseEntity<?> getCompanyWiseOpeningCount(
			@RequestParam(required = false, defaultValue = "0") Integer pageNo,
			@RequestParam(required = false, defaultValue = "10") Integer pageOffset) {
		return recruitmentService.getCompanyWiseOpeningCount(pageNo, pageOffset);
	}

	@GetMapping("/applicant-view/{id}")
	public ResponseEntity<?> getRecruitmentApplicantView(@PathVariable Long id) {
		return recruitmentService.getRecruitmentApplicantView(id);
	}

	@GetMapping("/filter")
	public ResponseEntity<?> getRecruitmentFilters() {
		return recruitmentService.getRecruitmentFilters();
	}

	@GetMapping("/name")
	public ResponseEntity<?> getRecruitmentByName(@RequestParam String name,
			@RequestParam(required = false, defaultValue = "0") Integer pageNo,
			@RequestParam(required = false, defaultValue = "10") Integer pageOffset) {
		return recruitmentService.getRecruitmentByName(name, pageNo, pageOffset);
	}

	@PostMapping(value = "/applicant/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> addApplicantDocument(@RequestParam("document") MultipartFile document,
			@RequestParam Long userId) {
		return recruitmentService.addApplicantDocument(document, userId);
	}

	@DeleteMapping("/applicant/document")
	public ResponseEntity<?> deleteApplicantDocument(@RequestParam Long userId, @RequestParam Long hrDocumentId) {
		return recruitmentService.deleteApplicantDocument(userId, hrDocumentId);
	}

	@PostMapping("/applicant/apply")
	public ResponseEntity<?> applyApplicantRecruitment(@RequestBody String application) {
		return recruitmentService.applyApplicantRecruitment(application);
	}

	@GetMapping("/has-applied")
	public ResponseEntity<?> hasApplicantApplied(@RequestParam Long recruitmentId, @RequestParam Long userId) {
		return recruitmentService.hasApplicantApplied(recruitmentId, userId);
	}

	@GetMapping("/applicant/applications")
	public ResponseEntity<?> getApplicantApplications(@RequestParam Long userId,
			@RequestParam(required = false, defaultValue = "0") Integer pageNo,
			@RequestParam(required = false, defaultValue = "10") Integer pageOffset,
			@RequestParam(required = false) String status) {
		return recruitmentService.getApplicantApplications(userId, pageNo, pageOffset, status);
	}

	@GetMapping("/applicant/application/with-status")
	public ResponseEntity<?> getApplicantApplicationWithStatus(@RequestParam Long userId,
			@RequestParam Long recruitmentId) {
		return recruitmentService.getApplicantApplicationWithStatus(userId, recruitmentId);
	}

	// New dashboard endpoints
	@GetMapping("/company-insights")
	public ResponseEntity<?> getCompanyInsights() {
		return recruitmentService.getCompanyInsights();
	}

	@GetMapping("/dashboard-stats")
	public ResponseEntity<?> getDashboardStats() {
		return recruitmentService.getDashboardStats();
	}
}
