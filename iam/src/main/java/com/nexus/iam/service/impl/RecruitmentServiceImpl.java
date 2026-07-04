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
                    "Please provide required params"
            );
        }
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getHrRecruitmentUrl()).queryParam("empId", empId);
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            ResponseEntity<?> response = restService.iamRestCall(builder.toUriString(), recruitment, headers, HttpMethod.POST, empId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "RecruitmentService",
                        "Failed to create recruitment",
                        "createRecruitment",
                        response.getStatusCode().toString(),
                        response.getBody() != null ? response.getBody().toString() : "No response body"
                );
            }
            return response;

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while creating recruitment",
                    "createRecruitment",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> getAllRecruitments(Long orgId, Boolean isActive, Long empId, String hiringType, String hiringStatus, Integer pageNo, Integer pageOffset) {
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
            ResponseEntity<?> response = restService.iamRestCall(builder.toUriString(), null, headers, HttpMethod.GET, empId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "RecruitmentService",
                        "Failed to fetch recruitments",
                        "getAllRecruitments",
                        response.getStatusCode().toString(),
                        response.getBody() != null ? response.getBody().toString() : "No response body"
                );
            }
            return response;

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while fetching recruitments",
                    "getAllRecruitments",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage()
            );
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
                    "Please provide required params"
            );
        }
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getHrRecruitmentUrl()).queryParam("empId", empId);
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            ResponseEntity<?> response = restService.iamRestCall(builder.toUriString(), recruitment, headers, HttpMethod.PUT, empId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "RecruitmentService",
                        "Failed to update recruitment",
                        "updateRecruitment",
                        response.getStatusCode().toString(),
                        response.getBody() != null ? response.getBody().toString() : "No response body"
                );
            }
            return response;

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while updating recruitment",
                    "updateRecruitment",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage()
            );
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
                    "Please provide required params"
            );
        }
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getHrRecruitmentUrl() + id);
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            ResponseEntity<?> response = restService.iamRestCall(builder.toUriString(), null, headers, HttpMethod.GET, null);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "RecruitmentService",
                        "Failed to fetch recruitment",
                        "getRecruitment",
                        response.getStatusCode().toString(),
                        response.getBody() != null ? response.getBody().toString() : "No response body"
                );
            }
            return response;

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while fetching recruitment",
                    "getRecruitment",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage()
            );
        }

    }

    @Override
    public ResponseEntity<?> getClosedRecruitments(Long orgId, Integer pageNo, Integer pageOffset) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getHrRecruitmentUrl() + "closed")
                    .queryParam("orgId", orgId)
                    .queryParam("pageNo", pageNo)
                    .queryParam("pageOffset", pageOffset);
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            ResponseEntity<?> response = restService.iamRestCall(builder.toUriString(), null, headers, HttpMethod.GET, null);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "RecruitmentService",
                        "Failed to fetch closed recruitments",
                        "getClosedRecruitments",
                        response.getStatusCode().toString(),
                        response.getBody() != null ? response.getBody().toString() : "No response body"
                );
            }
            return response;

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while fetching closed recruitments",
                    "getClosedRecruitments",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> createApplicantWithDocuments(Long recruitmentId, String applicant, MultipartFile resume, MultipartFile coverLetter) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getApplicantUrl())
                    .queryParam("recruitmentId", recruitmentId);
            Map<String, String> headers = new HashMap<>();
//            headers.put(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE);
            Map<String, Object> payload = new ConcurrentHashMap<>();
            payload.put("applicant", applicant);
            payload.put("resume", resume);
            payload.put("coverLetter", coverLetter);
            payload.put("recruitmentId", recruitmentId);
            ResponseEntity<?> response = restService.iamRestCall(builder.toUriString(), payload, headers, HttpMethod.POST, null);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "RecruitmentService",
                        "Failed to create applicant with documents",
                        "createApplicantWithDocuments",
                        response.getStatusCode().toString(),
                        response.getBody() != null ? response.getBody().toString() : "No response body"
                );
            }
            return response;

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while creating applicant with documents",
                    "createApplicantWithDocuments",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> getAllApplicants(Long recruitmentId, String status, String name, Character gender, Integer minAge, Integer maxAge, LocalDate appliedFromDate, LocalDate appliedToDate, Integer yearsOfExperience, Integer pageNo, Integer pageSize) {
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
            ResponseEntity<?> response = restService.iamRestCall(builder.toUriString(), null, headers, HttpMethod.GET, null);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "RecruitmentService",
                        "Failed to fetch applicants",
                        "getAllApplicants",
                        response.getStatusCode().toString(),
                        response.getBody() != null ? response.getBody().toString() : "No response body"
                );
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while fetching applicants",
                    "getAllApplicants",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage()
            );
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
                    "Please provide required params"
            );
        }
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getApplicantUrl() + "userId/" + id);
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            ResponseEntity<?> response = restService.iamRestCall(builder.toUriString(), null, headers, HttpMethod.GET, null);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "RecruitmentService",
                        "Failed to fetch applicant",
                        "getApplicantById",
                        response.getStatusCode().toString(),
                        response.getBody() != null ? response.getBody().toString() : "No response body"
                );
            }
            return response;

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while fetching applicant",
                    "getApplicantById",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage()
            );
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
                    "Please provide required params"
            );
        }
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getHrRecruitmentUrl() + "/analytics")
                    .queryParam("orgId", orgId);
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            ResponseEntity<?> response = restService.iamRestCall(builder.toUriString(), null, headers, HttpMethod.GET, null);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "RecruitmentService",
                        "Failed to fetch recruitment analytics",
                        "getRecruitmentAnalytics",
                        response.getStatusCode().toString(),
                        response.getBody() != null ? response.getBody().toString() : "No response body"
                );
            }
            return response;

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while fetching recruitment analytics",
                    "getRecruitmentAnalytics",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage()
            );
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
                    "Please provide required params"
            );
        }
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getApplicantUrl() + "/education")
                    .queryParam("userId", applicantId);
            Map<String, String> headers = commonUtils.buildJsonHeaders(null);
            ResponseEntity<?> response = restService.iamRestCall(builder.toUriString(), education, headers, HttpMethod.POST, null);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "RecruitmentService",
                        "Failed to add applicant education",
                        "addApplicantEducation",
                        response.getStatusCode().toString(),
                        response.getBody() != null ? response.getBody().toString() : "No response body"
                );
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while adding applicant education",
                    "addApplicantEducation",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage()
            );
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
                    "Please provide required params"
            );
        }
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getApplicantUrl() + "/experience")
                    .queryParam("userId", applicantId);
            Map<String, String> headers = commonUtils.buildJsonHeaders(null);
            ResponseEntity<?> response = restService.iamRestCall(builder.toUriString(), experience, headers, HttpMethod.POST, null);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "RecruitmentService",
                        "Failed to add applicant experience",
                        "addApplicantExperience",
                        response.getStatusCode().toString(),
                        response.getBody() != null ? response.getBody().toString() : "No response body"
                );
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while adding applicant experience",
                    "addApplicantExperience",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage()
            );
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
                    "Please provide required params"
            );
        }
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getApplicantUrl() + "/skill")
                    .queryParam("userId", applicantId);
            Map<String, String> headers = commonUtils.buildJsonHeaders(null);
            ResponseEntity<?> response = restService.iamRestCall(builder.toUriString(), skill, headers, HttpMethod.POST, null);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "RecruitmentService",
                        "Failed to add applicant skill",
                        "addApplicantSkill",
                        response.getStatusCode().toString(),
                        response.getBody() != null ? response.getBody().toString() : "No response body"
                );
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "RecruitmentService",
                    "Exception occurred while adding applicant skill",
                    "addApplicantSkill",
                    "INTERNAL_SERVER_ERROR",
                    e.getMessage()
            );
        }
    }
}
