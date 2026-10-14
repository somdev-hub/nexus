package com.nexus.iam.controller;

import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.nexus.iam.annotation.LogActivity;
import com.nexus.iam.utils.CommonUtils;
import com.nexus.iam.utils.RestService;
import com.nexus.iam.utils.WebConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/iam/pms/suite-payments")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PmsSuitePaymentController {

    private final RestService restService;
    private final WebConstants webConstants;
    private final CommonUtils commonUtils;

    @LogActivity("Create Suite Intent via IAM")
    @PostMapping("/intents")
    public ResponseEntity<?> createIntent(@RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String authToken,
            @RequestHeader(value = "X-Organization-ID", required = false) String orgIdHeader,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
        if (orgIdHeader != null)
            headers.put("X-Organization-ID", orgIdHeader);
        if (idempotencyKey != null)
            headers.put("Idempotency-Key", idempotencyKey);
        return restService.iamRestCall(webConstants.getPmsSuitePaymentsUrl() + "/intents", body, headers,
                HttpMethod.POST, null);
    }

    @LogActivity("Get Suite Intent By Ref via IAM")
    @GetMapping("/intents/by-ref")
    public ResponseEntity<?> getByRef(@RequestParam String ref,
            @RequestHeader("Authorization") String authToken,
            @RequestHeader(value = "X-Organization-ID", required = false) String orgIdHeader) {
        Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
        if (orgIdHeader != null)
            headers.put("X-Organization-ID", orgIdHeader);
        String url = UriComponentsBuilder.fromUriString(webConstants.getPmsSuitePaymentsUrl() + "/intents/by-ref")
                .queryParam("ref", ref).toUriString();
        return restService.iamRestCall(url, null, headers, HttpMethod.GET, null);
    }

    @LogActivity("Pay Suite Intent via IAM")
    @PostMapping("/intents/pay-by-ref")
    public ResponseEntity<?> payByRef(@RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String authToken,
            @RequestHeader(value = "X-Organization-ID", required = false) String orgIdHeader) {
        Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
        if (orgIdHeader != null)
            headers.put("X-Organization-ID", orgIdHeader);
        return restService.iamRestCall(webConstants.getPmsSuitePaymentsUrl() + "/intents/pay-by-ref", body, headers,
                HttpMethod.POST, null);
    }

    @LogActivity("Cancel Suite Intent via IAM")
    @PostMapping("/intents/by-ref/cancel")
    public ResponseEntity<?> cancel(@RequestBody Map<String, String> body,
            @RequestHeader("Authorization") String authToken,
            @RequestHeader(value = "X-Organization-ID", required = false) String orgIdHeader) {
        Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
        if (orgIdHeader != null)
            headers.put("X-Organization-ID", orgIdHeader);
        return restService.iamRestCall(webConstants.getPmsSuitePaymentsUrl() + "/intents/by-ref/cancel", body,
                headers, HttpMethod.POST, null);
    }

    @LogActivity("List Suite Intents via IAM")
    @GetMapping("/intents")
    public ResponseEntity<?> list(@RequestHeader("Authorization") String authToken,
            @RequestHeader(value = "X-Organization-ID", required = false) String orgIdHeader,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
        if (orgIdHeader != null)
            headers.put("X-Organization-ID", orgIdHeader);
        UriComponentsBuilder b = UriComponentsBuilder
                .fromUriString(webConstants.getPmsSuitePaymentsUrl() + "/intents")
                .queryParam("page", pageable.getPageNumber())
                .queryParam("size", pageable.getPageSize());
        if (orgId != null)
            b.queryParam("orgId", orgId);
        if (status != null)
            b.queryParam("status", status);
        return restService.iamRestCall(b.toUriString(), null, headers, HttpMethod.GET, null);
    }
}
