package com.nexus.hr.utils;

import com.nexus.hr.entity.HrLogs;
import com.nexus.hr.repository.HrLogsRepo;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class RestServices {

    private final HrLogsRepo hrLogsRepo;

    private final CommonUtils commonUtils;

    public RestServices(HrLogsRepo hrLogsRepo, CommonUtils commonUtils) {
        this.hrLogsRepo = hrLogsRepo;
        this.commonUtils = commonUtils;
    }

    public ResponseEntity<?> hrRestCall(String url, Object payload, Map<String, String> headers,
                                         HttpMethod method, Long hrId) {
        ResponseEntity<?> responseEntity = null;
        try {
            RestClient restClient = RestClient.create();
            RestClient.RequestBodySpec request = restClient.method(method).uri(url);

            if (headers != null) {
                headers.forEach(request::header);
            }

            if (ObjectUtils.isEmpty(payload)) {
                responseEntity = request.retrieve().toEntity(Object.class);
            } else {
                responseEntity = request.body(payload).retrieve().toEntity(Object.class);
            }
        } catch (Exception e) {
            responseEntity = new ResponseEntity<>("Exception occurred during REST call: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            HrLogs log = new HrLogs();
            log.setRequestUrl(url);
            log.setHttpMethod(method.name());
            log.setRequest(payload != null ? payload.toString() : null);
            if (responseEntity != null) {
                Object respBody = responseEntity.getBody();
                String responseString = respBody != null ? respBody.toString() : null;
                if (responseString != null) {
                    log.setResponse(commonUtils.jsonValidator(responseString));
                }
                log.setResponseStatus(responseEntity.getStatusCode().value());
            }
            log.setHrId(hrId != null ? hrId : 0L);

            hrLogsRepo.save(log);
        }

        return responseEntity;
    }
}
