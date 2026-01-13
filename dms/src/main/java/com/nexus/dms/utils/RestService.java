package com.nexus.dms.utils;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.nexus.dms.entities.DmsLogs;
import com.nexus.dms.repository.DmsLogsRepo;

@Service
public class RestService {

    @Autowired
    private DmsLogsRepo dmsLogsRepo;

    public ResponseEntity<?> dmsRestCall(String url, Object payload, Map<String, String> headers,
            HttpMethod method, Long userId) {
        ResponseEntity<?> responseEntity = null;
        try {
            RestClient restClient = RestClient.create();
            RestClient.RequestBodySpec request = restClient.method(method).uri(url);

            if (headers != null) {
                headers.forEach(request::header);
            }

            responseEntity = request.body(payload).retrieve().toEntity(Object.class);

        } catch (Exception e) {
            responseEntity = new ResponseEntity<>("Exception occurred during REST call: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            DmsLogs log = new DmsLogs();
            log.setRequestUrl(url);
            log.setHttpMethod(method.name());
            log.setRequest(payload.toString());
            log.setResponse(responseEntity != null ? responseEntity.toString() : "No Response");
            log.setResponseStatus(responseEntity != null ? responseEntity.getStatusCode().value() : 0);

            dmsLogsRepo.save(log);
        }

        return responseEntity;
    }

}
