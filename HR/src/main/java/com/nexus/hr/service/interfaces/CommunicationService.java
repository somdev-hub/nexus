package com.nexus.hr.service.interfaces;

import com.nexus.hr.payload.EmailCommunicationDto;
import org.springframework.http.ResponseEntity;

public interface CommunicationService {
    public ResponseEntity<?> sendCommunicationOverEmail(EmailCommunicationDto emailCommunicationDto);
}
