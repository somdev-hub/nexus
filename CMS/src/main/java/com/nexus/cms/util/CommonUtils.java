package com.nexus.cms.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
//import com.nexus.hr.model.entities.WOWOConfig;
import com.nexus.cms.chat.enums.AttachmentType;
import com.nexus.cms.exception.ServiceLevelException;
import com.nexus.cms.payload.RestPayload;
import com.nexus.cms.payload.TokenPayloadDto;
//import com.nexus.hr.service.interfaces.WOWOConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommonUtils {

    private final WebConstants webConstants;
    private final Object tokenLock = new Object(); // Lock for thread-safe token management
    private final Environment environment;
//    private final WOWOConfigService wowoConfigService;
    private String token;

    public boolean validateToken(String token) {
        String authUrl = webConstants.getVerifyTokenUrl();
        try {
            Map<String, String> body = Map.of("token", token.contains("Bearer ") ? token.substring(7) : token);
            RestClient restClient = RestClient.create();
            ResponseEntity<Map<String, String>> response = restClient.post().uri(authUrl)
                    .body(body)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<Map<String, String>>() {
                    });
            // extract isValid from response - fixed logic
            Map<String, String> responseBody = response.getBody();
            return response.getStatusCode().is2xxSuccessful() &&
                    !ObjectUtils.isEmpty(responseBody) &&
                    Boolean.parseBoolean(responseBody.get("isValid"));

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Thread-safe token retrieval with synchronized access to prevent concurrent
     * token generation.
     * Fixes race condition when multiple async tasks call getToken()
     * simultaneously.
     */
    public synchronized String getToken() {
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
            if (response.getStatusCode().is2xxSuccessful() && responseBody != null
                    && responseBody.containsKey("accessToken")) {
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
        try {
            jsonNode = objectMapper.readTree(jsonString);
            return objectMapper.writeValueAsString(jsonNode);
        } catch (Exception e) {
            return objectMapper.createObjectNode().put("message", jsonString).toString();
        }
    }

    public RestPayload buildRestPayload(String url, Map<String, String> queriesParams,
                                        Map<Integer, String> pathVariables, String headerType) {
        RestPayload restPayload = new RestPayload();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);

        // Append path variables (sorted by index) to the URL
        if (!ObjectUtils.isEmpty(pathVariables)) {
            pathVariables.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> builder.pathSegment(entry.getValue()));
        }

        // Append query parameters
        if (!ObjectUtils.isEmpty(queriesParams)) {
            queriesParams.forEach(builder::queryParam);
        }

        // Build headers
        Map<String, String> headers = new HashMap<>();
        if (!ObjectUtils.isEmpty(headerType)) {
            headers.put(CommonConstants.AUTHORIZATION, getToken());
            if (headerType.equalsIgnoreCase(CommonConstants.APPLICATION_JSON)) {
                headers.put(CommonConstants.CONTENT_TYPE, CommonConstants.APPLICATION_JSON);
            } else if (headerType.equalsIgnoreCase(CommonConstants.MULTIPART_FORM_DATA)) {
                headers.put(CommonConstants.CONTENT_TYPE, CommonConstants.MULTIPART_FORM_DATA);
            }
            // Add other header types if needed
        }

        restPayload.setBuilder(builder);
        restPayload.setHeaders(headers);

        return restPayload;

    }

//    public boolean isWiredOn(String wowoName) {
//        if (ObjectUtils.isEmpty(wowoName)) {
//            return false;
//        }
//        try {
//            String property = environment.getProperty("wowo." + wowoName + ".active");
//            if (!ObjectUtils.isEmpty(property)) {
//                return Boolean.parseBoolean(property);
//            }
//            WOWOConfig wowoConfig = wowoConfigService.getWOWOConfigByName(wowoName).getBody();
//            if (wowoConfig != null) {
//                return wowoConfig.getIsActive();
//            } else {
//                return false;
//            }
//        } catch (Exception e) {
//            return false;
//        }
//    }

    public AttachmentType validateAttachmentType(String contentType){

        return switch (contentType) {
            case "image/jpeg" -> AttachmentType.JPG;
            case "image/png" -> AttachmentType.PNG;
            case "image/gif" -> AttachmentType.GIF;
            case "video/mp4" -> AttachmentType.MP4;
            case "application/pdf" -> AttachmentType.PDF;
            case "audio/mpeg" -> AttachmentType.MP3;
            case "video/quicktime" -> AttachmentType.MOV;
            case "audio/wav" -> AttachmentType.WAV;
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> AttachmentType.DOCX;
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> AttachmentType.XLSX;
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> AttachmentType.PPTX;
            case "application/zip" -> AttachmentType.ZIP;
            default -> AttachmentType.OTHER;
        };
    }
}
