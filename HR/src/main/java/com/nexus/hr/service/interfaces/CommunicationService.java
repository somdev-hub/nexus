package com.nexus.hr.service.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nexus.hr.payload.EmailCommunicationDto;
import org.springframework.http.ResponseEntity;

public interface CommunicationService {
    ResponseEntity<?> sendCommunicationOverEmail(EmailCommunicationDto emailCommunicationDto);

    void sendCommunicationOverKafkaForCandidateSelection(EmailCommunicationDto emailCommunicationDto) throws JsonProcessingException;

    void sendCommunicationOverKafkaForPayroll(EmailCommunicationDto emailCommunicationDto);
}
