package com.nexus.iam.service.impl;

import com.nexus.iam.exception.ServiceLevelException;
import com.nexus.iam.service.NewChatService;
import com.nexus.iam.utils.RestService;
import com.nexus.iam.utils.WebConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of NewChatService
 * Wraps calls to CMS Chat microservice (v2)
 * Handles request forwarding, error handling, and response transformation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewChatServiceImpl implements NewChatService {

    private final RestService restService;
    private final WebConstants webConstants;

    @Override
    public ResponseEntity<?> createConversation(String request, MultipartFile avatar) {
        log.info("=== NewChatServiceImpl.createConversation called, avatar={} ===", avatar != null ? avatar.getOriginalFilename() : "null");
        log.info("=== request payload: {} ===", request);
        log.debug("Creating conversation");

        try {
            // Build URL with query parameters for participantId and orgId
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(webConstants.getCmsNewChatConversationUrl());

            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

            ResponseEntity<?> response = restService.iamRestCall(
                    builder.toUriString(),
                    request,
                    headers,
                    HttpMethod.POST,
                    null);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("CMS createConversation failed: status={}, body={}", response.getStatusCode(), response.getBody());
                return ResponseEntity.status(response.getStatusCode())
                        .body(response.getBody() != null ? response.getBody() : "{\"error\":\"Failed to create conversation\"}");
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "NewChatService",
                    "Failed to create conversation",
                    "createConversation",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getConversations(Long participantId, Long orgId) {
        log.debug("Fetching conversations for participantId: {}, orgId: {}", participantId, orgId);

        if (ObjectUtils.isEmpty(participantId) || ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Participant ID and Organization ID are required");
        }

        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(webConstants.getCmsNewChatConversationsUrl() + "/" + participantId)
                    .queryParam("orgId", orgId);

            ResponseEntity<?> response = restService.iamRestCall(
                    builder.toUriString(),
                    null,
                    Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE),
                    HttpMethod.GET,
                    participantId);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "NewChatService",
                        "Failed to fetch conversations",
                        "getConversations",
                        "HTTP " + response.getStatusCode().value(),
                        response.getBody() != null ? response.getBody().toString() : "Unknown error");
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "NewChatService",
                    "Failed to fetch conversations",
                    "getConversations",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getConversationDetails(Long conversationId, Long participantId, Long orgId) {
        log.debug("Fetching conversation details for conversationId: {}, participantId: {}, orgId: {}",
                conversationId, participantId, orgId);

        if (ObjectUtils.isEmpty(conversationId) || ObjectUtils.isEmpty(participantId) || ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest()
                    .body("Conversation ID, Participant ID, and Organization ID are required");
        }

        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(webConstants.getCmsNewChatConversationDetailsUrl() + "/" + conversationId)
                    .queryParam("participantId", participantId)
                    .queryParam("orgId", orgId);

            ResponseEntity<?> response = restService.iamRestCall(
                    builder.toUriString(),
                    null,
                    Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE),
                    HttpMethod.GET,
                    participantId);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "NewChatService",
                        "Failed to fetch conversation details",
                        "getConversationDetails",
                        "HTTP " + response.getStatusCode().value(),
                        response.getBody() != null ? response.getBody().toString() : "Unknown error");
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "NewChatService",
                    "Failed to fetch conversation details",
                    "getConversationDetails",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> updateConversation(String request, MultipartFile avatar, Long participantId, Long orgId) {
        log.debug("Updating conversation for participantId: {}, orgId: {}", participantId, orgId);

        if (ObjectUtils.isEmpty(participantId) || ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Participant ID and Organization ID are required");
        }

        try {
            String url = webConstants.getCmsNewChatConversationUrl();
            Map<String, Object> payload = new HashMap<>();
            payload.put("request", request);
            if (avatar != null) {
                payload.put("chatConversationAvatar", avatar);
            }

            ResponseEntity<?> response = restService.iamRestCall(
                    url,
                    payload,
                    Map.of(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE),
                    HttpMethod.PUT,
                    participantId);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "NewChatService",
                        "Failed to update conversation",
                        "updateConversation",
                        "HTTP " + response.getStatusCode().value(),
                        response.getBody() != null ? response.getBody().toString() : "Unknown error");
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "NewChatService",
                    "Failed to update conversation",
                    "updateConversation",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> markConversationAsViewed(Long participantId, Long conversationId, Long orgId) {
        log.debug("Marking conversation as viewed - participantId: {}, conversationId: {}, orgId: {}",
                participantId, conversationId, orgId);

        if (ObjectUtils.isEmpty(participantId) || ObjectUtils.isEmpty(conversationId) || ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest()
                    .body("Participant ID, Conversation ID, and Organization ID are required");
        }

        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(webConstants.getCmsNewChatViewUrl())
                    .queryParam("participantId", participantId)
                    .queryParam("conversationId", conversationId);

            ResponseEntity<?> response = restService.iamRestCall(
                    builder.toUriString(),
                    null,
                    Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE),
                    HttpMethod.POST,
                    participantId);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "NewChatService",
                        "Failed to mark conversation as viewed",
                        "markConversationAsViewed",
                        "HTTP " + response.getStatusCode().value(),
                        response.getBody() != null ? response.getBody().toString() : "Unknown error");
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "NewChatService",
                    "Failed to mark conversation as viewed",
                    "markConversationAsViewed",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> sendMessage(Object request, List<MultipartFile> files, Long participantId, Long orgId) {
        log.debug("Sending message from participantId: {}, orgId: {}", participantId, orgId);

        if (ObjectUtils.isEmpty(participantId) || ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Participant ID and Organization ID are required");
        }

        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(webConstants.getCmsNewChatMessageUrl())
                    .queryParam("participantId", participantId)
                    .queryParam("orgId", orgId);

            Map<String, Object> payload = new HashMap<>();
            ObjectMapper mapper = new ObjectMapper();
            if (request instanceof String) {
                try {
                    // Parse JSON string to JsonNode to avoid double-encoding
                    payload.put("message", mapper.readTree((String) request));
                } catch (Exception e) {
                    log.warn("[SEND_MESSAGE] Failed to parse message JSON, sending as string: ", e);
                    payload.put("message", request);
                }
            } else {
                payload.put("message", request);
            }

            if (files != null && !files.isEmpty()) {
                payload.put("files", files);
            }

            ResponseEntity<?> response = restService.iamRestCall(
                    builder.toUriString(),
                    payload,
                    Map.of(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE),
                    HttpMethod.POST,
                    participantId);

            log.info("[SEND_MESSAGE] CMS response: status={}, body={}", response.getStatusCode(), response.getBody());

            // Forward the actual CMS response (including error details) instead of wrapping in generic exception
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("[SEND_MESSAGE] CMS returned error: status={}, body={}", response.getStatusCode(), response.getBody());
                return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
            }
            return response;
        } catch (RuntimeException e) {
            log.error("[SEND_MESSAGE] Runtime exception: ", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Internal server error",
                    "message", e.getMessage(),
                    "method", "sendMessage"));
        }
    }

    @Override
    public ResponseEntity<?> uploadMultimedia(MultipartFile[] files, Long participantId, Long orgId) {
        log.debug("Uploading {} multimedia files for participantId: {}, orgId: {}", files.length, participantId, orgId);

        if (ObjectUtils.isEmpty(participantId) || ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Participant ID and Organization ID are required");
        }

        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body("No files provided for upload");
        }

        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(webConstants.getCmsNewChatMultimediaUrl())
                    .queryParam("participantId", participantId)
                    .queryParam("orgId", orgId);

            Map<String, Object> payload = new HashMap<>();
            for (int i = 0; i < files.length; i++) {
                payload.put("files", files[i]);
            }

            ResponseEntity<?> response = restService.iamRestCall(
                    builder.toUriString(),
                    payload,
                    Map.of(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE),
                    HttpMethod.POST,
                    participantId);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "NewChatService",
                        "Failed to upload multimedia",
                        "uploadMultimedia",
                        "HTTP " + response.getStatusCode().value(),
                        response.getBody() != null ? response.getBody().toString() : "Unknown error");
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "NewChatService",
                    "Failed to upload multimedia",
                    "uploadMultimedia",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> editMessage(String request, Long participantId, Long orgId) {
        log.debug("Editing message from participantId: {}, orgId: {}", participantId, orgId);

        if (ObjectUtils.isEmpty(participantId) || ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Participant ID and Organization ID are required");
        }

        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(webConstants.getCmsNewChatMessageUrl())
                    .queryParam("participantId", participantId)
                    .queryParam("orgId", orgId);

            ResponseEntity<?> response = restService.iamRestCall(
                    builder.toUriString(),
                    request,
                    Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE),
                    HttpMethod.PUT,
                    participantId);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "NewChatService",
                        "Failed to edit message",
                        "editMessage",
                        "HTTP " + response.getStatusCode().value(),
                        response.getBody() != null ? response.getBody().toString() : "Unknown error");
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "NewChatService",
                    "Failed to edit message",
                    "editMessage",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getConversationMessages(Long conversationId, Long participantId, Long beforeId, Long limit,
            Long orgId) {
        log.debug("Fetching messages for conversationId: {}, participantId: {}, beforeId: {}, limit: {}",
                conversationId, participantId, beforeId, limit);

        if (ObjectUtils.isEmpty(conversationId) || ObjectUtils.isEmpty(participantId) || ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest()
                    .body("Conversation ID, Participant ID, and Organization ID are required");
        }

        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(webConstants.getCmsNewChatMessagesUrl())
                    .queryParam("conversationId", conversationId)
                    .queryParam("participantId", participantId)
                    .queryParam("limit", limit != null ? limit : 50)
                    .queryParam("orgId", orgId);

            if (beforeId != null) {
                builder.queryParam("beforeId", beforeId);
            }

            ResponseEntity<?> response = restService.iamRestCall(
                    builder.toUriString(),
                    null,
                    Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE),
                    HttpMethod.GET,
                    participantId);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "NewChatService",
                        "Failed to fetch conversation messages",
                        "getConversationMessages",
                        "HTTP " + response.getStatusCode().value(),
                        response.getBody() != null ? response.getBody().toString() : "Unknown error");
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "NewChatService",
                    "Failed to fetch conversation messages",
                    "getConversationMessages",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getPresenceStatuses(java.util.List<Long> userIds) {
        log.debug("Fetching presence statuses for {} userIds", userIds != null ? userIds.size() : 0);

        if (ObjectUtils.isEmpty(userIds)) {
            return ResponseEntity.badRequest().body("At least one userId is required");
        }

        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(webConstants.getCmsChatPresenceBatchUrl());

            for (Long userId : userIds) {
                builder.queryParam("userIds", userId);
            }

            ResponseEntity<?> response = restService.iamRestCall(
                    builder.toUriString(),
                    null,
                    Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE),
                    HttpMethod.GET,
                    null);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "NewChatService",
                        "Failed to fetch presence statuses",
                        "getPresenceStatuses",
                        "HTTP " + response.getStatusCode().value(),
                        response.getBody() != null ? response.getBody().toString() : "Unknown error");
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "NewChatService",
                    "Failed to fetch presence statuses",
                    "getPresenceStatuses",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> addParticipants(Long conversationId, String request, Long participantId, Long orgId) {
        if (ObjectUtils.isEmpty(conversationId) || ObjectUtils.isEmpty(participantId) || ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Conversation ID, Participant ID, and Organization ID are required");
        }
        try {
            String url = webConstants.getCmsNewChatConversationUrl() + "/" + conversationId + "/participants";
            ResponseEntity<?> response = restService.iamRestCall(
                    url,
                    request,
                    Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE),
                    HttpMethod.POST,
                    participantId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException("NewChatService", "Failed to add participants", "addParticipants",
                        "HTTP " + response.getStatusCode().value(),
                        response.getBody() != null ? response.getBody().toString() : "Unknown error");
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException("NewChatService", "Failed to add participants", "addParticipants",
                    e.getClass().getSimpleName(), e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> removeParticipant(Long conversationId, Long participantIdToRemove, Long participantId, Long orgId) {
        if (ObjectUtils.isEmpty(conversationId) || ObjectUtils.isEmpty(participantId) || ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Conversation ID, Participant ID, and Organization ID are required");
        }
        try {
            String url = webConstants.getCmsNewChatConversationUrl() + "/" + conversationId + "/participants/" + participantIdToRemove;
            ResponseEntity<?> response = restService.iamRestCall(
                    url,
                    null,
                    Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE),
                    HttpMethod.DELETE,
                    participantId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException("NewChatService", "Failed to remove participant", "removeParticipant",
                        "HTTP " + response.getStatusCode().value(),
                        response.getBody() != null ? response.getBody().toString() : "Unknown error");
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException("NewChatService", "Failed to remove participant", "removeParticipant",
                    e.getClass().getSimpleName(), e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> deleteConversation(Long conversationId, Long participantId, Long orgId) {
        if (ObjectUtils.isEmpty(conversationId) || ObjectUtils.isEmpty(participantId) || ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Conversation ID, Participant ID, and Organization ID are required");
        }
        try {
            String url = webConstants.getCmsNewChatConversationUrl() + "/" + conversationId + "?participantId=" + participantId;
            ResponseEntity<?> response = restService.iamRestCall(
                    url,
                    null,
                    Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE),
                    HttpMethod.DELETE,
                    participantId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException("NewChatService", "Failed to delete conversation", "deleteConversation",
                        "HTTP " + response.getStatusCode().value(),
                        response.getBody() != null ? response.getBody().toString() : "Unknown error");
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException("NewChatService", "Failed to delete conversation", "deleteConversation",
                    e.getClass().getSimpleName(), e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> clearChatForSelf(Long conversationId, Long participantId, Long orgId) {
        if (ObjectUtils.isEmpty(conversationId) || ObjectUtils.isEmpty(participantId) || ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Conversation ID, Participant ID, and Organization ID are required");
        }
        try {
            String url = webConstants.getCmsNewChatConversationUrl() + "/" + conversationId + "/clear?participantId=" + participantId;
            ResponseEntity<?> response = restService.iamRestCall(
                    url,
                    null,
                    Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE),
                    HttpMethod.POST,
                    participantId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException("NewChatService", "Failed to clear chat", "clearChatForSelf",
                        "HTTP " + response.getStatusCode().value(),
                        response.getBody() != null ? response.getBody().toString() : "Unknown error");
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException("NewChatService", "Failed to clear chat", "clearChatForSelf",
                    e.getClass().getSimpleName(), e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> downloadChat(Long conversationId, Long participantId, Long orgId) {
        if (ObjectUtils.isEmpty(conversationId) || ObjectUtils.isEmpty(participantId) || ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Conversation ID, Participant ID, and Organization ID are required");
        }
        try {
            String url = webConstants.getCmsNewChatConversationUrl() + "/" + conversationId + "/download?participantId=" + participantId;
            ResponseEntity<?> response = restService.iamRestCall(
                    url,
                    null,
                    Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE),
                    HttpMethod.GET,
                    participantId);
            if (response != null && response.getBody() != null) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8")
                        .body(response.getBody());
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException("NewChatService", "Failed to download chat", "downloadChat",
                    e.getClass().getSimpleName(), e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> searchMessages(Long conversationId, Long participantId, String query, Long orgId) {
        if (ObjectUtils.isEmpty(conversationId) || ObjectUtils.isEmpty(participantId) || ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Conversation ID, Participant ID, and Organization ID are required");
        }
        try {
            String url = webConstants.getCmsNewChatConversationUrl() + "/" + conversationId + "/search?participantId=" + participantId + "&query=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
            ResponseEntity<?> response = restService.iamRestCall(
                    url,
                    null,
                    Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE),
                    HttpMethod.GET,
                    participantId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException("NewChatService", "Failed to search messages", "searchMessages",
                        "HTTP " + response.getStatusCode().value(),
                        response.getBody() != null ? response.getBody().toString() : "Unknown error");
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException("NewChatService", "Failed to search messages", "searchMessages",
                    e.getClass().getSimpleName(), e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> setAdmin(Long conversationId, Long participantId, Long adminId, boolean isAdmin, Long orgId) {
        if (ObjectUtils.isEmpty(conversationId) || ObjectUtils.isEmpty(participantId) || ObjectUtils.isEmpty(adminId) || ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Conversation ID, Participant ID, Admin ID, and Organization ID are required");
        }
        try {
            String url = webConstants.getCmsNewChatConversationUrl() + "/" + conversationId + "/participants/" + participantId + "/admin?adminId=" + adminId + "&isAdmin=" + isAdmin;
            ResponseEntity<?> response = restService.iamRestCall(
                    url,
                    null,
                    Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE),
                    HttpMethod.PUT,
                    adminId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException("NewChatService", "Failed to set admin status", "setAdmin",
                        "HTTP " + response.getStatusCode().value(),
                        response.getBody() != null ? response.getBody().toString() : "Unknown error");
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException("NewChatService", "Failed to set admin status", "setAdmin",
                    e.getClass().getSimpleName(), e.getMessage());
        }
    }
}
