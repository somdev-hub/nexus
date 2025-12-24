package com.nexus.iam.utils;

import java.sql.Timestamp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.iam.entities.Logs;
import com.nexus.iam.repository.LogsRepo;

@Service
public class Logger {

    @Autowired
    private LogsRepo logsRepo;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public void saveLog(Logs log) {
        try {
            logsRepo.save(log);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void log(String requestUrl, HttpMethod httpMethod, HttpStatusCode responseStatus, Object request,
            Object response, Long userId) {
        try {

            Logs log = new Logs();
            log.setRequestUrl(requestUrl);
            log.setHttpMethod(httpMethod.name());
            log.setResponseStatus(responseStatus.value());
            log.setRequest(request != null ? objectMapper.writeValueAsString(request) : null);
            log.setResponse(response != null ? objectMapper.writeValueAsString(response) : null);
            log.setUserId(userId);
            log.setCreatedOn(new Timestamp(System.currentTimeMillis()));

            saveLog(log);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
