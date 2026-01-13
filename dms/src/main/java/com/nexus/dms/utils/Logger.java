package com.nexus.dms.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.dms.entities.DmsLogs;
import com.nexus.dms.exception.ServiceLevelException;
import com.nexus.dms.repository.DmsLogsRepo;

@Service
public class Logger {

    @Autowired
    private DmsLogsRepo dmsLogsRepo;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public void saveLogs(String requestUrl, HttpMethod httpMethod, HttpStatus httpStatus, Object request,
            Object response, Long documentRecordId) throws JsonProcessingException {
        try {
            DmsLogs dmsLogs = new DmsLogs();
            dmsLogs.setRequestUrl(requestUrl);
            dmsLogs.setHttpMethod(httpMethod.name());
            dmsLogs.setResponseStatus(httpStatus.value());
            dmsLogs.setRequest(request != null ? objectMapper.writeValueAsString(request) : null);
            dmsLogs.setResponse(response != null ? objectMapper.writeValueAsString(response) : null);
            dmsLogs.setDocumentRecordId(documentRecordId);
            dmsLogsRepo.save(dmsLogs);
        } catch (Exception e) {
            throw new ServiceLevelException("Logger", "Failed to save logs", "saveLogs", e.getClass().getSimpleName(),
                    e.getLocalizedMessage());
        }
    }

}
