package com.nexus.iam.service.impl;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.nexus.iam.exception.ServiceLevelException;
import com.nexus.iam.service.RecruitmentService;
import com.nexus.iam.utils.CommonUtils;
import com.nexus.iam.utils.RestService;
import com.nexus.iam.utils.WebConstants;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecruitmentServiceImpl implements RecruitmentService {

	private final WebConstants webConstants;
	private final RestService restService;
	private final CommonUtils commonUtils;

	@Override
	public ResponseEntity<?> createRecruitment(String recruitment, Long empId) {

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getHrRecruitmentUrl())
				.queryParam("empId", empId);
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), recruitment, headers,
				HttpMethod.POST, empId);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to create recruitment",
					"createRecruitment",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> getAllRecruitments(Long orgId, Boolean isActive, Long empId, String hiringType,
			String hiringStatus, Integer pageNo, Integer pageOffset) {

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getHrRecruitmentUrl())
				.queryParam("orgId", orgId)
				.queryParam("isActive", isActive)
				.queryParam("empId", empId)
				.queryParam("hiringType", hiringType)
				.queryParam("hiringStatus", hiringStatus)
				.queryParam("pageNo", pageNo)
				.queryParam("pageOffset", pageOffset);
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		ResponseEntity<?> response = restService.iamRestCall(builder.toUriString(), null, headers, HttpMethod.GET,
				empId);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to fetch recruitments",
					"getAllRecruitments",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody().toString() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> updateRecruitment(String recruitment, Long empId) {

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getHrRecruitmentUrl())
				.queryParam("empId", empId);
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		ResponseEntity<?> response = restService.iamRestCall(builder.toUriString(), recruitment, headers,
				HttpMethod.PUT, empId);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to update recruitment",
					"updateRecruitment",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody().toString() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> getRecruitment(Long id) {

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getHrRecruitmentUrl() + id);
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, headers,
				HttpMethod.GET, null);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to fetch recruitment",
					"getRecruitment",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> getClosedRecruitments(Long orgId, Integer pageNo, Integer pageOffset) {

		UriComponentsBuilder builder = UriComponentsBuilder
				.fromUriString(webConstants.getHrRecruitmentUrl() + "closed")
				.queryParam("orgId", orgId)
				.queryParam("pageNo", pageNo)
				.queryParam("pageOffset", pageOffset);
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, headers,
				HttpMethod.GET, null);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to fetch closed recruitments",
					"getClosedRecruitments",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> createApplicantWithDocuments(Long recruitmentId, String applicant, MultipartFile resume,
			MultipartFile coverLetter) {

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getApplicantUrl())
				.queryParam("recruitmentId", recruitmentId);
		Map<String, String> headers = new HashMap<>();
		// headers.put(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE);
		Map<String, Object> payload = new ConcurrentHashMap<>();
		payload.put("applicant", applicant);
		payload.put("resume", resume);
		payload.put("coverLetter", coverLetter);
		payload.put("recruitmentId", recruitmentId);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), payload, headers,
				HttpMethod.POST, null);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to create applicant with documents",
					"createApplicantWithDocuments",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> getAllApplicants(Long recruitmentId, String status, String name, Character gender,
			Integer minAge, Integer maxAge, LocalDate appliedFromDate, LocalDate appliedToDate,
			Integer yearsOfExperience, Integer pageNo, Integer pageSize) {

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getApplicantUrl())
				.queryParam("recruitmentId", recruitmentId)
				.queryParam("status", status)
				.queryParam("name", name)
				.queryParam("gender", gender)
				.queryParam("minAge", minAge)
				.queryParam("maxAge", maxAge)
				.queryParam("appliedFromDate", appliedFromDate)
				.queryParam("appliedToDate", appliedToDate)
				.queryParam("yearsOfExperience", yearsOfExperience)
				.queryParam("pageNo", pageNo)
				.queryParam("pageSize", pageSize);
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, headers,
				HttpMethod.GET, null);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to fetch applicants",
					"getAllApplicants",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> getApplicantById(Long id) {

		UriComponentsBuilder builder = UriComponentsBuilder
				.fromUriString(webConstants.getApplicantUrl() + "userId/" + id);
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, headers,
				HttpMethod.GET, null);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to fetch applicant",
					"getApplicantById",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> getRecruitmentAnalytics(Long orgId) {

		UriComponentsBuilder builder = UriComponentsBuilder
				.fromUriString(webConstants.getHrRecruitmentUrl() + "/analytics")
				.queryParam("orgId", orgId);
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, headers,
				HttpMethod.GET, null);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to fetch recruitment analytics",
					"getRecruitmentAnalytics",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> addApplicantEducation(String education, Long applicantId) {

		UriComponentsBuilder builder = UriComponentsBuilder
				.fromUriString(webConstants.getApplicantUrl() + "/education")
				.queryParam("userId", applicantId);
		Map<String, String> headers = commonUtils.buildJsonHeaders(null);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), education, headers,
				HttpMethod.POST, null);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to add applicant education",
					"addApplicantEducation",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> addApplicantExperience(String experience, Long applicantId) {

		UriComponentsBuilder builder = UriComponentsBuilder
				.fromUriString(webConstants.getApplicantUrl() + "/experience")
				.queryParam("userId", applicantId);
		Map<String, String> headers = commonUtils.buildJsonHeaders(null);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), experience, headers,
				HttpMethod.POST, null);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to add applicant experience",
					"addApplicantExperience",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> addApplicantSkill(String skill, Long applicantId) {

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getApplicantUrl() + "/skill")
				.queryParam("userId", applicantId);
		Map<String, String> headers = commonUtils.buildJsonHeaders(null);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), skill, headers,
				HttpMethod.POST, null);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to add applicant skill",
					"addApplicantSkill",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> updateApplicant(String payload, Long userId) {

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getApplicantUrl())
				.queryParam("userId", userId);
		Map<String, String> headers = commonUtils.buildJsonHeaders(null);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), payload, headers,
				HttpMethod.PUT, userId);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to update applicant",
					"updateApplicant",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> getOpeningsToday(Integer pageNo, Integer pageOffset, String status, String orgName,
			String location, String query) {

		UriComponentsBuilder builder = UriComponentsBuilder
				.fromUriString(webConstants.getHrRecruitmentUrl() + "/openings-today")
				.queryParam("pageNo", pageNo)
				.queryParam("pageOffset", pageOffset)
				.queryParam("status", status)
				.queryParam("orgName", orgName)
				.queryParam("location", location)
				.queryParam("query", query);
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, headers,
				HttpMethod.GET, null);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to fetch today's openings",
					"getOpeningsToday",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody().toString() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> getOpeningsBeforeToday(Integer pageNo, Integer pageOffset, String status, String orgName,
			String location, String query) {

		UriComponentsBuilder builder = UriComponentsBuilder
				.fromUriString(webConstants.getHrRecruitmentUrl() + "/openings-before-today")
				.queryParam("pageNo", pageNo)
				.queryParam("pageOffset", pageOffset)
				.queryParam("status", status)
				.queryParam("orgName", orgName)
				.queryParam("location", location)
				.queryParam("query", query);
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, headers,
				HttpMethod.GET, null);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to fetch openings before today",
					"getOpeningsBeforeToday",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> getPositionPieGraph() {

		UriComponentsBuilder builder = UriComponentsBuilder
				.fromUriString(webConstants.getHrRecruitmentUrl() + "/position-pie-graph");
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, headers,
				HttpMethod.GET, null);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to fetch position pie graph",
					"getPositionPieGraph",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> getExperienceWiseOpenings() {

		UriComponentsBuilder builder = UriComponentsBuilder
				.fromUriString(webConstants.getHrRecruitmentUrl() + "/openings-experience-wise");
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, headers,
				HttpMethod.GET, null);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to fetch experience-wise openings",
					"getExperienceWiseOpenings",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> getCompanyWiseOpeningCount(Integer pageNo, Integer pageOffset) {

		UriComponentsBuilder builder = UriComponentsBuilder
				.fromUriString(webConstants.getHrRecruitmentUrl() + "/company-wise-opening-count")
				.queryParam("pageNo", pageNo)
				.queryParam("pageOffset", pageOffset);
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, headers,
				HttpMethod.GET, null);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to fetch company-wise opening count",
					"getCompanyWiseOpeningCount",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> getRecruitmentApplicantView(Long id) {

		UriComponentsBuilder builder = UriComponentsBuilder
				.fromUriString(webConstants.getHrRecruitmentUrl() + "/applicant-view/" + id);
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, headers,
				HttpMethod.GET, null);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to fetch recruitment applicant view",
					"getRecruitmentApplicantView",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> getRecruitmentFilters() {

		UriComponentsBuilder builder = UriComponentsBuilder
				.fromUriString(webConstants.getHrRecruitmentUrl() + "/filter");
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, headers,
				HttpMethod.GET, null);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to fetch recruitment filters",
					"getRecruitmentFilters",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> getRecruitmentByName(String name, Integer pageNo, Integer pageOffset) {

		UriComponentsBuilder builder = UriComponentsBuilder
				.fromUriString(webConstants.getHrRecruitmentUrl() + "/name")
				.queryParam("name", name)
				.queryParam("pageNo", pageNo)
				.queryParam("pageOffset", pageOffset);
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, headers,
				HttpMethod.GET, null);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to fetch recruitment by name",
					"getRecruitmentByName",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> addApplicantDocument(MultipartFile document, Long userId) {

		UriComponentsBuilder builder = UriComponentsBuilder
				.fromUriString(webConstants.getApplicantUrl() + "/document")
				.queryParam("userId", userId);
		Map<String, String> headers = new HashMap<>();
		Map<String, Object> payload = new ConcurrentHashMap<>();
		payload.put("document", document);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), payload, headers,
				HttpMethod.POST, userId);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to add applicant document",
					"addApplicantDocument",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> deleteApplicantDocument(Long userId, Long hrDocumentId) {

		UriComponentsBuilder builder = UriComponentsBuilder
				.fromUriString(webConstants.getApplicantUrl() + "/document")
				.queryParam("userId", userId)
				.queryParam("hrDocumentId", hrDocumentId);
		Map<String, String> headers = new HashMap<>();
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, headers,
				HttpMethod.DELETE, userId);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to delete applicant document",
					"deleteApplicantDocument",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> applyApplicantRecruitment(String application) {

		UriComponentsBuilder builder = UriComponentsBuilder
				.fromUriString(webConstants.getHrRecruitmentUrl() + "/applicant/apply");
		Map<String, String> headers = commonUtils.buildJsonHeaders(null);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), application, headers,
				HttpMethod.POST, null);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to apply applicant recruitment",
					"applyApplicantRecruitment",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> hasApplicantApplied(Long recruitmentId, Long userId) {

		UriComponentsBuilder builder = UriComponentsBuilder
				.fromUriString(webConstants.getHrRecruitmentUrl() + "/has-applied")
				.queryParam("recruitmentId", recruitmentId)
				.queryParam("userId", userId);
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, headers,
				HttpMethod.GET, null);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to check if applicant has applied",
					"hasApplicantApplied",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> getApplicantApplications(Long userId, Integer pageNo, Integer pageOffset, String status) {

		UriComponentsBuilder builder = UriComponentsBuilder
				.fromUriString(webConstants.getHrRecruitmentUrl() + "/applicant/applications")
				.queryParam("userId", userId)
				.queryParam("pageNo", pageNo)
				.queryParam("pageOffset", pageOffset)
				.queryParam("status", status);
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, headers,
				HttpMethod.GET, null);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to fetch applicant applications",
					"getApplicantApplications",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> getApplicantApplicationWithStatus(Long userId, Long recruitmentId) {

		UriComponentsBuilder builder = UriComponentsBuilder
				.fromUriString(webConstants.getHrRecruitmentUrl() + "/applicant/application/with-status")
				.queryParam("userId", userId)
				.queryParam("recruitmentId", recruitmentId);
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, headers,
				HttpMethod.GET, null);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to fetch applicant application with status",
					"getApplicantApplicationWithStatus",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> getCompanyInsights() {

		UriComponentsBuilder builder = UriComponentsBuilder
				.fromUriString(webConstants.getHrRecruitmentUrl() + "/company-insights");
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, headers,
				HttpMethod.GET, null);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to fetch company insights",
					"getCompanyInsights",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> getDashboardStats() {

		UriComponentsBuilder builder = UriComponentsBuilder
				.fromUriString(webConstants.getHrRecruitmentUrl() + "/dashboard-stats");
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, headers,
				HttpMethod.GET, null);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to fetch dashboard stats",
					"getDashboardStats",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;

	}

	@Override
	public ResponseEntity<?> getApplicantByApplicantId(Long id) {
		
		UriComponentsBuilder builder = UriComponentsBuilder
				.fromUriString(webConstants.getApplicantUrl() + "/" + id);
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, headers,
				HttpMethod.GET, null);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to fetch applicant by applicantId",
					"getApplicantByApplicantId",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;
	}

	@Override
	public ResponseEntity<?> getApplicantByRecruitmentMapping(Long mappingId) {
		UriComponentsBuilder builder = UriComponentsBuilder
				.fromUriString(webConstants.getHrRecruitmentUrl() + "/applicant/recruitment-mapping/" + mappingId);
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, headers,
				HttpMethod.GET, null);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to fetch applicant by recruitment mapping",
					"getApplicantByRecruitmentMapping",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;
	}

	@Override
	public ResponseEntity<?> updateApplicantRecruitmentStatus(Long mappingId, String status) {
		UriComponentsBuilder builder = UriComponentsBuilder
				.fromUriString(webConstants.getHrRecruitmentUrl() + "/applicant/recruitment-mapping/" + mappingId + "/status")
				.queryParam("status", status);
		Map<String, String> headers = new HashMap<>();
		headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, headers,
				HttpMethod.PUT, null);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new ServiceLevelException(
					"RecruitmentService",
					"Failed to update applicant recruitment status",
					"updateApplicantRecruitmentStatus",
					response.getStatusCode().toString(),
					response.getBody() != null ? response.getBody() : "No response body");
		}
		return response;
	}
}