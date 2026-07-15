package com.nexus.iam.service.impl;

import com.nexus.iam.exception.ServiceLevelException;
import com.nexus.iam.service.RecruitmentService;
import com.nexus.iam.utils.CommonUtils;
import com.nexus.iam.utils.RestService;
import com.nexus.iam.utils.WebConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RecruitmentServiceImpl implements RecruitmentService {

    private final WebConstants webConstants;
    private final RestService restService;
    private final CommonUtils commonUtils;

    @Override
    public ResponseEntity<?> createRecruitment(String recruitment, Long empId) {
        if (ObjectUtils.isEmpty(recruitment) || ObjectUtils.isEmpty(empId)) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Required params missing",
                    "createRecruitment",
                    "BAD_REQUEST",
                    "Please provide required params");
        }
        try {
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

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while creating recruitment",
                    "createRecruitment",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getAllRecruitments(Long orgId, Boolean isActive, Long empId, String hiringType,
            String hiringStatus, Integer pageNo, Integer pageOffset) {
        try {
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

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while fetching recruitments",
                    "getAllRecruitments",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> updateRecruitment(String recruitment, Long empId) {
        if (ObjectUtils.isEmpty(recruitment) || ObjectUtils.isEmpty(empId)) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Required params missing",
                    "updateRecruitment",
                    "BAD_REQUEST",
                    "Please provide required params");
        }
        try {
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

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while updating recruitment",
                    "updateRecruitment",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }

    }

    @Override
    public ResponseEntity<?> getRecruitment(Long id) {
        if (ObjectUtils.isEmpty(id)) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Required params missing",
                    "getRecruitment",
                    "BAD_REQUEST",
                    "Please provide required params");
        }
        try {
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

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while fetching recruitment",
                    "getRecruitment",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }

    }

    @Override
    public ResponseEntity<?> getClosedRecruitments(Long orgId, Integer pageNo, Integer pageOffset) {
        try {
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

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while fetching closed recruitments",
                    "getClosedRecruitments",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> createApplicantWithDocuments(Long recruitmentId, String applicant, MultipartFile resume,
            MultipartFile coverLetter) {
        try {
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

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while creating applicant with documents",
                    "createApplicantWithDocuments",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getAllApplicants(Long recruitmentId, String status, String name, Character gender,
            Integer minAge, Integer maxAge, LocalDate appliedFromDate, LocalDate appliedToDate,
            Integer yearsOfExperience, Integer pageNo, Integer pageSize) {
        try {
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
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while fetching applicants",
                    "getAllApplicants",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getApplicantById(Long id) {
        if (ObjectUtils.isEmpty(id)) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Required params missing",
                    "getApplicantById",
                    "BAD_REQUEST",
                    "Please provide required params");
        }
        try {
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

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while fetching applicant",
                    "getApplicantById",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getRecruitmentAnalytics(Long orgId) {
        if (ObjectUtils.isEmpty(orgId)) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Required params missing",
                    "getRecruitmentAnalytics",
                    "BAD_REQUEST",
                    "Please provide required params");
        }
        try {
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

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while fetching recruitment analytics",
                    "getRecruitmentAnalytics",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> addApplicantEducation(String education, Long applicantId) {
        if (ObjectUtils.isEmpty(education) || ObjectUtils.isEmpty(applicantId)) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Required params missing",
                    "addApplicantEducation",
                    "BAD_REQUEST",
                    "Please provide required params");
        }
        try {
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
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while adding applicant education",
                    "addApplicantEducation",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> addApplicantExperience(String experience, Long applicantId) {
        if (ObjectUtils.isEmpty(experience) || ObjectUtils.isEmpty(applicantId)) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Required params missing",
                    "addApplicantExperience",
                    "BAD_REQUEST",
                    "Please provide required params");
        }
        try {
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
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while adding applicant experience",
                    "addApplicantExperience",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> addApplicantSkill(String skill, Long applicantId) {
        if (ObjectUtils.isEmpty(skill) || ObjectUtils.isEmpty(applicantId)) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Required params missing",
                    "addApplicantSkill",
                    "BAD_REQUEST",
                    "Please provide required params");
        }
        try {
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
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while adding applicant skill",
                    "addApplicantSkill",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> updateApplicant(String payload, Long userId) {
        if (ObjectUtils.isEmpty(payload) || ObjectUtils.isEmpty(userId)) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Required params missing",
                    "updateApplicant",
                    "BAD_REQUEST",
                    "Please provide required params");
        }
        try {
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
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while updating applicant",
                    "updateApplicant",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getOpeningsToday(Integer pageNo, Integer pageOffset, String status, String orgName,
            String location, String query) {
        try {
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
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while fetching today's openings",
                    "getOpeningsToday",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getOpeningsBeforeToday(Integer pageNo, Integer pageOffset, String status, String orgName,
            String location, String query) {
        try {
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
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while fetching openings before today",
                    "getOpeningsBeforeToday",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getPositionPieGraph() {
        try {
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
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while fetching position pie graph",
                    "getPositionPieGraph",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getExperienceWiseOpenings() {
        try {
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
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while fetching experience-wise openings",
                    "getExperienceWiseOpenings",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getCompanyWiseOpeningCount(Integer pageNo, Integer pageOffset) {
        try {
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
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while fetching company-wise opening count",
                    "getCompanyWiseOpeningCount",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getRecruitmentApplicantView(Long id) {
        if (ObjectUtils.isEmpty(id)) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Required params missing",
                    "getRecruitmentApplicantView",
                    "BAD_REQUEST",
                    "Please provide required params");
        }
        try {
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

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while fetching recruitment applicant view",
                    "getRecruitmentApplicantView",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getRecruitmentFilters() {
        try {
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

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while fetching recruitment filters",
                    "getRecruitmentFilters",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getRecruitmentByName(String name, Integer pageNo, Integer pageOffset) {
        try {
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

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while fetching recruitment by name",
                    "getRecruitmentByName",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> addApplicantDocument(MultipartFile document, Long userId) {
        if (ObjectUtils.isEmpty(document) || ObjectUtils.isEmpty(userId)) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Required params missing",
                    "addApplicantDocument",
                    "BAD_REQUEST",
                    "Please provide required params");
        }
        try {
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
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while adding applicant document",
                    "addApplicantDocument",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> deleteApplicantDocument(Long userId, Long hrDocumentId) {
        if (ObjectUtils.isEmpty(userId) || ObjectUtils.isEmpty(hrDocumentId)) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Required params missing",
                    "deleteApplicantDocument",
                    "BAD_REQUEST",
                    "Please provide required params");
        }
        try {
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
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while deleting applicant document",
                    "deleteApplicantDocument",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> applyApplicantRecruitment(String application) {
        if (ObjectUtils.isEmpty(application)) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Required params missing",
                    "applyApplicantRecruitment",
                    "BAD_REQUEST",
                    "Please provide required params");
        }
        try {
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
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while applying applicant recruitment",
                    "applyApplicantRecruitment",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> hasApplicantApplied(Long recruitmentId, Long userId) {
        if (ObjectUtils.isEmpty(recruitmentId) || ObjectUtils.isEmpty(userId)) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Required params missing",
                    "hasApplicantApplied",
                    "BAD_REQUEST",
                    "Please provide required params");
        }
        try {
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
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while checking if applicant has applied",
                    "hasApplicantApplied",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getApplicantApplications(Long userId, Integer pageNo, Integer pageOffset, String status) {
        if (ObjectUtils.isEmpty(userId)) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Required params missing",
                    "getApplicantApplications",
                    "BAD_REQUEST",
                    "Please provide required params");
        }
        try {
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
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while fetching applicant applications",
                    "getApplicantApplications",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getApplicantApplicationWithStatus(Long userId, Long recruitmentId) {
        if (ObjectUtils.isEmpty(userId) || ObjectUtils.isEmpty(recruitmentId)) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Required params missing",
                    "getApplicantApplicationWithStatus",
                    "BAD_REQUEST",
                    "Please provide required params");
        }
        try {
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
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while fetching applicant application with status",
                    "getApplicantApplicationWithStatus",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getCompanyInsights() {
        try {
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
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while fetching company insights",
                    "getCompanyInsights",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getDashboardStats() {
        try {
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
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while fetching dashboard stats",
                    "getDashboardStats",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage());
        }
    }
}