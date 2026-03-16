package com.nexus.cms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@RequiredArgsConstructor
@Service
public class EmailCommunicationService {

    private final ObjectMapper  objectMapper;

    public void handleEmailCommunication(String message){
        try{
            @SuppressWarnings("unchecked")
            Map<String,Object> kafkaContent = objectMapper.readValue(message, Map.class);
//            Map<String >
            if (kafkaContent.containsKey("message")){

            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
