package com.nexus.hr.utils;

import com.nexus.hr.payload.TokenPayloadDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Service
public class CommonUtils {

    private String token;

    private final WebConstants webConstants;

    public CommonUtils(WebConstants webConstants) {
        this.webConstants = webConstants;
    }

    public boolean validateToken(String token) {
        String authUrl = webConstants.getVerifyTokenUrl();
        try {
            Map<String, String> body = Map.of("token", token.substring(7));
            RestClient restClient = RestClient.create();
            ResponseEntity<Map<String, String>> response = restClient.post().uri(authUrl)
                    .body(body)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<Map<String, String>>() {
                    });
            // extract isValid from response
            Map<String, String> responseBody = response.getBody();
            return !response.getStatusCode().is2xxSuccessful() ||
                    ObjectUtils.isEmpty(responseBody) ||
                    !Boolean.parseBoolean(responseBody.get("isValid"));

        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    public String getToken() {
        if (this.token == null || !validateToken(this.token)) {
            this.token = generateToken();
        }
        return this.token;
    }

    public String generateToken() {
        String authUrl = webConstants.getGenerateTokenUrl();
        Map<String, String> body = new HashMap<>();
        body.put("email", webConstants.getGenericUserId());
        body.put("password", webConstants.getGenericPassword());
        try {
            RestClient restClient = RestClient.create();
            ResponseEntity<Map<String, String>> response = restClient.post().uri(authUrl)
                    .body(body)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<Map<String, String>>() {
                    });
            // extract token from response
            Map<String, String> responseBody = response.getBody();
            if (response.getStatusCode().is2xxSuccessful() && responseBody != null && responseBody.containsKey("accessToken")) {
                return "Bearer " + responseBody.get("accessToken");
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public TokenPayloadDto decryptToken(String token) {
        String authUrl = webConstants.getDecryptTokenUrl();
        Map<String, String> body = Map.of("token", token.substring(7));
        try {
            RestClient restClient = RestClient.create();
            ResponseEntity<TokenPayloadDto> response = restClient.post().uri(authUrl)
                    .body(body)
                    .retrieve()
                    .toEntity(TokenPayloadDto.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String jsonValidator(String jsonString) {
        if (ObjectUtils.isEmpty(jsonString)) {
            return "{}";
        }
        JsonNode jsonNode = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try{
            jsonNode = objectMapper.readTree(jsonString);
        }
        catch (JacksonException _){
            jsonNode = objectMapper.createObjectNode().put("message", jsonString);
        }
        return objectMapper.writeValueAsString(jsonNode);
    }
}
