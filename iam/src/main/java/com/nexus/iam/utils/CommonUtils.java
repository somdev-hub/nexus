package com.nexus.iam.utils;

import com.nexus.iam.dto.LoginRequest;
import com.nexus.iam.dto.LoginResponse;
import com.nexus.iam.security.JwtUtil;
import com.nexus.iam.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CommonUtils {

    private final JwtUtil jwtUtil;
    private final ObjectProvider<AuthenticationService> authenticationServiceProvider;
    private final ObjectMapper objectMapper;
    private final WebConstants webConstants;

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

    public Map<String,String> buildJsonHeaders(String authToken){
        Map<String,String> headers=new ConcurrentHashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if(!ObjectUtils.isEmpty(authToken)){
            headers.put(HttpHeaders.AUTHORIZATION, authToken);
        }else{
            AuthenticationService authenticationService = authenticationServiceProvider.getIfAvailable();
            if(authenticationService != null) {
                LoginResponse loginResponse = authenticationService.authenticate(new LoginRequest(webConstants.getGenericUserId(),
                        webConstants.getGenericPassword()));
                headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + loginResponse.getAccessToken());
            }
        }


        return headers;
    }
}
