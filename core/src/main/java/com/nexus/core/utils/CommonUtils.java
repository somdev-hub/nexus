package com.nexus.core.utils;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import com.nexus.core.entities.Logs;

public class CommonUtils {

    @Autowired
    private Logger logger;

    @Autowired
    private CommonConstants commonConstants;

    public ResponseEntity<?> coreRestCall(String url, Map<String, String> headers, Object body, HttpMethod method,
            Long orgId) {

        try {
            RestClient restClient = RestClient.create();
            RestClient.RequestBodySpec request = restClient.method(method).uri(url);

            if (headers != null) {
                headers.forEach(request::header);
            }

            ResponseEntity<?> response = request.body(body).retrieve().toEntity(Object.class);
            Logs log = new Logs();
            log.setRequestUrl(url);
            log.setHttpMethod(method.name());
            log.setRequest(body);
            log.setResponse(response.getBody());
            log.setResponseStatus(response.getStatusCode().value());
            log.setOrg(orgId);
            logger.log(log);
            return response;
        } catch (Exception e) {

            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    public Boolean validateToken(String token) {
        String authUrl = commonConstants.getVerifyTokenUrl();
        Map<String, String> body = Map.of("token", token.substring(7));
        try {
            RestClient restClient = RestClient.create();
            ResponseEntity<Map<String, String>> response = restClient.post().uri(authUrl)
                    .body(body)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<Map<String, String>>() {
                    });
            // extract isValid from response
            return response.getStatusCode().is2xxSuccessful() &&
                    response.getBody() != null &&
                    Boolean.parseBoolean(response.getBody().get("isValid"));

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String generateToken() {
        String authUrl = commonConstants.getGenerateTokenUrl();
        Map<String, String> body = new HashMap<>();
        body.put("email", "user123@nexus.com");
        body.put("password", "generic");
        try {
            RestClient restClient = RestClient.create();
            ResponseEntity<Map<String, String>> response = restClient.post().uri(authUrl)
                    .body(body)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<Map<String, String>>() {
                    });
            // extract token from response
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().get("accessToken");
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
