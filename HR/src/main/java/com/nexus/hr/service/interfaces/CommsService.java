package com.nexus.hr.service.interfaces;

import com.nexus.hr.payload.CommsPayload;
import com.nexus.hr.payload.EmailAttachmentDto;
import com.nexus.hr.utils.CommonConstants;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface CommsService {
    ResponseEntity<?> sendCommunication(CommonConstants.CommsTriggerPoint triggerPoint, List<EmailAttachmentDto> attachments, CommsPayload commsPayload);
}
