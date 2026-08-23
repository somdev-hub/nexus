package com.nexus.hr.service.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nexus.hr.payload.EmailCommunicationDto;
import org.springframework.http.ResponseEntity;

public interface CommunicationService {

    /**
     * @deprecated This method is deprecated and will be removed in future versions. Please use sendCommunicationOverKafka instead.
     */
    @Deprecated
    ResponseEntity<?> sendCommunicationOverEmail(EmailCommunicationDto emailCommunicationDto);

    /**
     * @deprecated This method is deprecated and will be removed in future versions. Please use sendCommunicationOverKafka instead.
     */
    @Deprecated
    void sendCommunicationOverKafkaForCandidateSelection(EmailCommunicationDto emailCommunicationDto) throws JsonProcessingException;

    /**
     * @deprecated This method is deprecated and will be removed in future versions. Please use sendCommunicationOverKafka instead.
     */
    @Deprecated
    void sendCommunicationOverKafkaForPayroll(EmailCommunicationDto emailCommunicationDto);

    void sendCommunicationOverKafka(EmailCommunicationDto emailCommunicationDto);
}
