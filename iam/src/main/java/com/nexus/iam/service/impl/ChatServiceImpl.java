package com.nexus.iam.service.impl;

import com.nexus.iam.entities.User;
import com.nexus.iam.exception.ResourceNotFoundException;
import com.nexus.iam.exception.ServiceLevelException;
import com.nexus.iam.repository.UserRepository;
import com.nexus.iam.service.ChatService;
import com.nexus.iam.utils.RestService;
import com.nexus.iam.utils.WebConstants;
import lombok.RequiredArgsConstructor;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@RequiredArgsConstructor
@Service
public class ChatServiceImpl implements ChatService {

    private final RestService restService;
    private final WebConstants webConstants;
    private final UserRepository userRepository;

    @Override
    public ResponseEntity<?> createConversation(String request, Long userId) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getCmsConversationUrl()).queryParam("userId", userId);
            ResponseEntity<?> response = restService.iamRestCall(builder.toUriString(), request, Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), HttpMethod.POST, userId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "ChatService",
                        "Failed to create conversation",
                        "createConversation",
                        "HTTP " + response.getStatusCode().value(),
                        "Response: " + response.getBody());
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "ChatService",
                    "Failed to create conversation",
                    "createConversation",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getUserConversations(int page, int size, Long orgId, Long userId) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getCmsConversationUrl())
                    .queryParam("page", page)
                    .queryParam("size", size)
                    .queryParam("orgId", orgId)
                    .queryParam("userId", userId);
            ResponseEntity<?> response = restService.iamRestCall(builder.toUriString(), null, Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), HttpMethod.GET, userId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "ChatService",
                        "Failed to get user conversations",
                        "getUserConversations",
                        "HTTP " + response.getStatusCode().value(),
                        "Response: " + response.getBody());
            }
            JSONObject cmsResponse = new JSONObject(response.getBody().toString());
            JSONArray content = cmsResponse.optJSONArray("content");
            if (content == null) {
                return response;
            }
            for (int i = 0; i < content.length(); i++) {
                JSONObject conversation = content.getJSONObject(i);
                long participantId = conversation.optLong("participantId", 0L);
                if (participantId != userId) {
                    User participant = userRepository.findById(participantId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", participantId));
                    conversation.put("participantName", participant.getName());
                    conversation.put("participantEmail", participant.getEmail());
                    conversation.put("participantAvatarUrl", participant.getProfilePhoto());
                    conversation.put("participantRole",
                            participant.getRoles().stream().findFirst().map(role -> role.getName()).orElse("USER"));
                }
                content.put(i, conversation);
            }
            cmsResponse.put("content", content);
            return new ResponseEntity<>(cmsResponse.toString(), response.getHeaders(), response.getStatusCode());
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "ChatService",
                    "Failed to get user conversations",
                    "getUserConversations",
                    e.getClass().getSimpleName(),
                    e.getMessage());

        }

    }

    @Override
    public ResponseEntity<?> getConversation(String conversationId, Long orgId, Long userId) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(webConstants.getCmsConversationUrl() + "/" + conversationId)
                    .queryParam("orgId", orgId)
                    .queryParam("userId", userId);
            ResponseEntity<?> response = restService.iamRestCall(builder.toUriString(), null,
                    Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), HttpMethod.GET, userId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "ChatService",
                        "Failed to get conversation",
                        "getConversation",
                        "HTTP " + response.getStatusCode().value(),
                        "Response: " + response.getBody());
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "ChatService",
                    "Failed to get conversation",
                    "getConversation",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getConversationMessages(String conversationId, Long orgId, int page, int size,
            Long userId) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(webConstants.getCmsConversationUrl() + "/" + conversationId + "/messages")
                    .queryParam("orgId", orgId)
                    .queryParam("page", page)
                    .queryParam("size", size)
                    .queryParam("userId", userId);
            ResponseEntity<?> response = restService.iamRestCall(builder.toUriString(), null,
                    Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), HttpMethod.GET, userId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "ChatService",
                        "Failed to get conversation messages",
                        "getConversationMessages",
                        "HTTP " + response.getStatusCode().value(),
                        "Response: " + response.getBody());
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "ChatService",
                    "Failed to get conversation messages",
                    "getConversationMessages",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> addParticipant(String conversationId, String request, Long orgId) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(webConstants.getCmsConversationUrl() + "/" + conversationId + "/participants")
                    .queryParam("orgId", orgId);
            ResponseEntity<?> response = restService.iamRestCall(builder.toUriString(), request,
                    Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), HttpMethod.POST, null);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "ChatService",
                        "Failed to add participant",
                        "addParticipant",
                        "HTTP " + response.getStatusCode().value(),
                        "Response: " + response.getBody());
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "ChatService",
                    "Failed to add participant",
                    "addParticipant",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> removeParticipant(String conversationId, Long userId, Long orgId) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(
                            webConstants.getCmsConversationUrl() + "/" + conversationId + "/participants/" + userId)
                    .queryParam("conversationId", conversationId)
                    .queryParam("userId", userId)
                    .queryParam("orgId", orgId);
            ResponseEntity<?> response = restService.iamRestCall(builder.toUriString(), null,
                    Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), HttpMethod.DELETE, null);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "ChatService",
                        "Failed to remove participant",
                        "removeParticipant",
                        "HTTP " + response.getStatusCode().value(),
                        "Response: " + response.getBody());
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "ChatService",
                    "Failed to remove participant",
                    "removeParticipant",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> sendMessage(String request, Long orgId, Long userId) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getCmsMessageUrl())
                    .queryParam("orgId", orgId)
                    .queryParam("userId", userId);
            ResponseEntity<?> response = restService.iamRestCall(builder.toUriString(), request,
                    Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), HttpMethod.POST, userId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "ChatService",
                        "Failed to send message",
                        "sendMessage",
                        "HTTP " + response.getStatusCode().value(),
                        "Response: " + response.getBody());
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "ChatService",
                    "Failed to send message",
                    "sendMessage",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getMessage(String messageId, Long orgId) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(webConstants.getCmsMessageUrl() + "/" + messageId)
                    .queryParam("messageId", messageId)
                    .queryParam("orgId", orgId);
            ResponseEntity<?> response = restService.iamRestCall(builder.toUriString(), null,
                    Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), HttpMethod.GET, null);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "ChatService",
                        "Failed to get message",
                        "getMessage",
                        "HTTP " + response.getStatusCode().value(),
                        "Response: " + response.getBody());
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "ChatService",
                    "Failed to get message",
                    "getMessage",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getConversationStats(String conversationId, Long orgId) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(webConstants.getCmsConversationUrl() + "/" + conversationId + "/stats")
                    .queryParam("conversationId", conversationId)
                    .queryParam("orgId", orgId);
            ResponseEntity<?> response = restService.iamRestCall(builder.toUriString(), null,
                    Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE), HttpMethod.GET, null);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "ChatService",
                        "Failed to get conversation stats",
                        "getConversationStats",
                        "HTTP " + response.getStatusCode().value(),
                        "Response: " + response.getBody());
            }
            return response;
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "ChatService",
                    "Failed to get conversation stats",
                    "getConversationStats",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }
}
