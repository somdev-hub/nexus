package com.nexus.cms.service;

import com.nexus.cms.model.entities.CMSLogs;
import com.nexus.cms.model.enums.CommsStatus;
import com.nexus.cms.model.enums.CommsType;
import com.nexus.cms.repository.CMSLogsRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoggerService {

    private final CMSLogsRepo cmsLogsRepo;

    public void log(String requestService, List<String> recipientEmails, List<String> ccEmails, List<String> bccEmails, List<String> phoneNumbers, CommsType commsType, String message, String uuid, CommsStatus status) {
        try {
            CMSLogs cmsLogs = new CMSLogs();
            cmsLogs.setRequestService(requestService);
            cmsLogs.setRecipientEmails(recipientEmails);
            cmsLogs.setCcEmails(ccEmails);
            cmsLogs.setBccEmails(bccEmails);
            cmsLogs.setPhoneNumbers(phoneNumbers);
            cmsLogs.setCommsType(commsType);
            cmsLogs.setMessage(message);
            cmsLogs.setUuid(uuid);
            cmsLogs.setStatus(status);
            cmsLogs.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            cmsLogsRepo.save(cmsLogs);
        } catch (RuntimeException e) {
            log.error("Failed to log communication: {}", e.getMessage());
        }
    }
}
