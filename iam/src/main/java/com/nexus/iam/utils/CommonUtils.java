package com.nexus.iam.utils;

import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class CommonUtils {

    public String jsonValidator(String jsonString) {
        if (ObjectUtils.isEmpty(jsonString)) {
            return "{}";
        }
        JsonNode jsonNode = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            jsonNode = objectMapper.readTree(jsonString);
        } catch (JacksonException _) {
            jsonNode = objectMapper.createObjectNode().put("message", jsonString);
        }
        return objectMapper.writeValueAsString(jsonNode);
    }
}
