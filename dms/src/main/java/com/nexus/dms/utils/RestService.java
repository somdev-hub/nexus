package com.nexus.dms.utils;

import java.util.Map;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestClient;

import com.nexus.dms.entities.DmsLogs;
import com.nexus.dms.repository.DmsLogsRepo;

@Service
public class RestService {

    private final DmsLogsRepo dmsLogsRepo;

    private final CommonUtils commonUtils;

    public RestService(DmsLogsRepo dmsLogsRepo, CommonUtils commonUtils) {
        this.dmsLogsRepo = dmsLogsRepo;
        this.commonUtils = commonUtils;
    }

    public ResponseEntity<?> dmsRestCall(String url, Object payload, Map<String, String> headers,
                                         HttpMethod method, Long documentRecordId) {
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
            DmsLogs log = new DmsLogs();
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
            log.setDocumentRecordId(documentRecordId != null ? documentRecordId : 0L);

            dmsLogsRepo.save(log);
        }

        return responseEntity;
    }

}
