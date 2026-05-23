package com.nexus.iam.service.impl;

import com.nexus.iam.exception.ServiceLevelException;
import com.nexus.iam.service.NewChatService;
import com.nexus.iam.utils.RestService;
import com.nexus.iam.utils.WebConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

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
    public ResponseEntity<?> createConversation(String request, MultipartFile avatar, Long participantId, Long orgId) {
        log.debug("Creating conversation for participantId: {}, orgId: {}", participantId, orgId);

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
                    HttpMethod.POST,
                    participantId);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "NewChatService",
                        "Failed to create conversation",
                        "createConversation",
                        "HTTP " + response.getStatusCode().value(),
                        response.getBody() != null ? response.getBody().toString() : "Unknown error");
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
    public ResponseEntity<?> sendMessage(String request, Long participantId, Long orgId) {
        log.debug("Sending message from participantId: {}, orgId: {}", participantId, orgId);

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
                    Map.of(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE),
                    HttpMethod.POST,
                    participantId);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "NewChatService",
                        "Failed to send message",
                        "sendMessage",
                        "HTTP " + response.getStatusCode().value(),
                        response.getBody() != null ? response.getBody().toString() : "Unknown error");
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "NewChatService",
                    "Failed to send message",
                    "sendMessage",
                    e.getClass().getSimpleName(),
                    e.getMessage());
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
}
