package com.nexus.cms.chat.service.implementations;

import com.nexus.cms.chat.entities.*;
import com.nexus.cms.chat.enums.ChatMessageStatus;
import com.nexus.cms.chat.model.ChatMessageRequestDto;
import com.nexus.cms.chat.model.ConversationMessagesDto;
import com.nexus.cms.chat.repositories.ChatConversationRepo;
import com.nexus.cms.chat.repositories.ChatMessageIndividualStatusRepo;
import com.nexus.cms.chat.repositories.ChatMessageRepo;
import com.nexus.cms.chat.service.interfaces.MessageService;
import com.nexus.cms.exception.ResourceNotFoundException;
import com.nexus.cms.exception.ServiceLevelException;
import com.nexus.cms.util.CommonConstants;
import com.nexus.cms.util.CommonUtils;
import com.nexus.cms.util.RestService;
import com.nexus.cms.util.StompPrincipalNames;
import com.nexus.cms.util.WebConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageServiceImpl implements MessageService {

    private final RestService restService;
    private final ChatConversationRepo chatConversationRepo;
    private final WebConstants webConstants;
    private final ModelMapper modelMapper;
    private final CommonUtils commonUtils;
    private final ChatMessageRepo chatMessageRepo;
    private final ChatMessageIndividualStatusRepo chatMessageIndividualStatusRepo;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public ResponseEntity<ConversationMessagesDto> sendMessage(ChatMessageRequestDto message, List<MultipartFile> files,
            Long participantId) {
        try {
            ChatConversation chatConversation = chatConversationRepo.findByIdWithParticipants(message.getChatConversationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "ChatConversation",
                            "id",
                            message.getChatConversationId()));

            // Authorization check - ensure participant is in conversation
            boolean isParticipant = chatConversation.getChatConversationParticipants()
                    .stream()
                    .anyMatch(p -> p.getParticipantId().equals(participantId));

            if (!isParticipant) {
                return ResponseEntity.status(403).build();
            }

            ChatConversationParticipant chatConversationParticipant = chatConversation.getChatConversationParticipants()
                    .stream().filter(participant -> participant.getParticipantId().equals(message.getParticipantId()))
                    .findFirst().orElseThrow(() -> new ResourceNotFoundException(
                            "ChatConversationParticipant",
                            "userId",
                            message.getParticipantId()));

            // Idempotency: if a message with the same clientMessageId already exists, return it
            if (message.getClientMessageId() != null && !message.getClientMessageId().isBlank()) {
                java.util.Optional<ChatMessage> existing = chatMessageRepo
                        .findByChatConversation_ChatConversationIdAndClientMessageId(
                                message.getChatConversationId(), message.getClientMessageId());
                if (existing.isPresent()) {
                    ChatMessage existingMessage = existing.get();
                    ConversationMessagesDto dto = modelMapper.map(existingMessage, ConversationMessagesDto.class);
                    ChatConversationParticipant sender = existingMessage.getChatConversationParticipant();
                    if (sender != null) {
                        ConversationMessagesDto.ParticipantInfo pi = new ConversationMessagesDto.ParticipantInfo();
                        pi.setParticipantId(sender.getParticipantId());
                        pi.setParticipantName(sender.getParticipantName());
                        pi.setParticipantEmail(sender.getParticipantEmail());
                        pi.setParticipantAvatar(sender.getParticipantAvatar());
                        dto.setChatConversationParticipant(pi);
                    }
                    dto.setChatConversationId(chatConversation.getChatConversationId());
                    return ResponseEntity.ok(dto);
                }
            }

            ChatMessage chatMessage = modelMapper.map(message, ChatMessage.class);
            chatMessage.setChatMessageStatus(ChatMessageStatus.SENT);
            chatMessage.setChatConversationParticipant(chatConversationParticipant);
            chatMessage.setChatConversation(chatConversation);

            chatMessage = chatMessageRepo.save(chatMessage);
            ChatMessage finalChatMessage1 = chatMessage;
            List<ChatMessageIndividualStatus> chatMessageIndividualStatuses = chatConversation
                    .getChatConversationParticipants().stream().map(participant -> {
                        if (!participant.equals(chatConversationParticipant)) {
                            ChatMessageIndividualStatus status = new ChatMessageIndividualStatus();
                            status.setParticipant(participant);
                            status.setChatMessage(finalChatMessage1);
                            status.setStatus(ChatMessageStatus.SENT);
                            return status;
                        }

                        return null;
                    }).filter(Objects::nonNull).toList();

            chatMessageIndividualStatusRepo.saveAll(chatMessageIndividualStatuses);

            ChatMessage finalChatMessage = chatMessage;
            List<ChatMessageAttachment> attachments = new ArrayList<>();
            if (!ObjectUtils.isEmpty(message.getChatMessageAttachmentList())) {
                message.getChatMessageAttachmentList()
                        .forEach(attachment -> attachment.setChatMessage(finalChatMessage));
                attachments.addAll(message.getChatMessageAttachmentList());
            }

            if (!ObjectUtils.isEmpty(files)) {
                Map<String, String> headers = new ConcurrentHashMap<>();
                headers.put(CommonConstants.CONTENT_TYPE, CommonConstants.MULTIPART_FORM_DATA);
                headers.put(CommonConstants.AUTHORIZATION, commonUtils.getToken());
                UriComponentsBuilder url = UriComponentsBuilder.fromUriString(
                        webConstants.getDmsIndividualDocumentUploadUrl());

                files.forEach(file -> {
                    Map<String, Object> payload = new ConcurrentHashMap<>();
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("userId", chatConversationParticipant.getParticipantId());
                    dto.put("fileName", file.getOriginalFilename());
                    dto.put("remarks", "conversation_" + chatConversation.getChatConversationId() + "_message_"
                            + finalChatMessage.getChatMessageId() + "_" + new Timestamp(System.currentTimeMillis()));
                    dto.put("documentType", "CHAT_MESSAGE_ATTACHMENT");

                    payload.put("file", file);
                    payload.put("dto", dto);

                    ResponseEntity<?> dmsResponse = restService.cmsRestCall(url.toUriString(), payload, headers,
                            HttpMethod.POST, chatConversation.getOrgId());
                    if (dmsResponse.getStatusCode().is2xxSuccessful() && !ObjectUtils.isEmpty(dmsResponse.getBody())) {
                        String documentUrl = commonUtils.parseAndGetDocumentUrl(dmsResponse);
                        ChatMessageAttachment chatMessageAttachment = new ChatMessageAttachment();
                        chatMessageAttachment.setChatMessage(finalChatMessage);
                        chatMessageAttachment.setFilePath(documentUrl);
                        chatMessageAttachment.setFileName(file.getOriginalFilename());
                        chatMessageAttachment.setAttachmentType(
                                commonUtils.validateAttachmentType(Objects.requireNonNull(file.getContentType())));
                        attachments.add(chatMessageAttachment);
                    }
                });

            }
            chatMessage.setChatMessageAttachmentList(attachments);
            chatMessage = chatMessageRepo.save(chatMessage);

            // Keep conversation metadata (ordering/preview) in sync with the latest message
            try {
                chatConversation.setLastMessageAt(chatMessage.getSentAt());
                chatConversation.setLastMessageText(chatMessage.getChatMessageText());
                chatConversation.setLastMessageSenderId(chatConversationParticipant.getParticipantId());
                chatConversation.setLastMessageSenderName(chatConversationParticipant.getParticipantName());
                chatConversation.setTotalMessages((chatConversation.getTotalMessages() == null ? 0L : chatConversation.getTotalMessages()) + 1L);
                chatConversation.setLastModifiedAt(new Timestamp(System.currentTimeMillis()));
                chatConversationRepo.save(chatConversation);
            } catch (Exception e) {
                log.warn("Failed to update conversation metadata after send", e);
            }

            // Push a chat notification to every other participant's private queue
            // so it surfaces in the header notification bell in real time.
            try {
                String senderName = chatConversationParticipant.getParticipantName();
                String title = "New message from " + (senderName != null ? senderName : "a participant");
                String body = chatMessage.getChatMessageText() != null ? chatMessage.getChatMessageText() : "Sent a message";
                for (ChatConversationParticipant participant : chatConversation.getChatConversationParticipants()) {
                    if (participant.getParticipantId().equals(participantId)) {
                        continue;
                    }
                    Long participantOrgId = participant.getOrgId() != null ? participant.getOrgId() : chatConversation.getOrgId();
                    String principalName = StompPrincipalNames.of(participant.getParticipantId(), participantOrgId);
                    Map<String, Object> notification = new HashMap<>();
                    notification.put("notificationId", chatMessage.getChatMessageId());
                    notification.put("eventId", "chat-" + chatMessage.getChatMessageId());
                    notification.put("eventType", "CHAT_MESSAGE");
                    notification.put("entityType", "CHAT");
                    notification.put("entityId", chatConversation.getChatConversationId());
                    notification.put("entityRef", String.valueOf(chatConversation.getChatConversationId()));
                    notification.put("title", title);
                    notification.put("body", body);
                    notification.put("severity", "INFO");
                    notification.put("targetOrgId", participantOrgId);
                    notification.put("facts", Map.of("conversationId", chatConversation.getChatConversationId()));
                    notification.put("createdAt", chatMessage.getSentAt() != null ? chatMessage.getSentAt().toInstant().toString() : java.time.Instant.now().toString());

                    Map<String, Object> push = new HashMap<>();
                    push.put("type", "MESSAGE");
                    push.put("message", notification);

                    messagingTemplate.convertAndSendToUser(principalName, "/queue/nexus/notifications", push);
                }
            } catch (Exception e) {
                log.warn("Failed to push chat notifications", e);
            }

            // Map to DTO with participant info
            ConversationMessagesDto conversationMessagesDto = modelMapper.map(chatMessage, ConversationMessagesDto.class);
            ConversationMessagesDto.ParticipantInfo participantInfo = new ConversationMessagesDto.ParticipantInfo();
            participantInfo.setParticipantId(chatConversationParticipant.getParticipantId());
            participantInfo.setParticipantName(chatConversationParticipant.getParticipantName());
            participantInfo.setParticipantEmail(chatConversationParticipant.getParticipantEmail());
            participantInfo.setParticipantAvatar(chatConversationParticipant.getParticipantAvatar());
            conversationMessagesDto.setChatConversationParticipant(participantInfo);
            conversationMessagesDto.setChatConversationId(chatConversation.getChatConversationId());

            return ResponseEntity.ok(conversationMessagesDto);
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "MessageServiceImpl",
                    "Failed to send message",
                    "sendMesssage",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> editMessage(ChatMessageRequestDto message) {
        try {
            ChatMessage chatMessage = chatMessageRepo.findById(message.getChatMessageId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "ChatMessage",
                            "id",
                            message.getChatMessageId()));
            chatMessage.setChatMessageText(message.getChatMessageText());
            chatMessage.setIsEdited(true);
            chatMessage = chatMessageRepo.save(chatMessage);
            return ResponseEntity.ok(chatMessage);
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "MessageServiceImpl",
                    "Failed to edit message",
                    "editMesssage",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> sendMultimediaMessage(List<MultipartFile> files, Long participantId) {
        if (ObjectUtils.isEmpty(files) || ObjectUtils.isEmpty(participantId)) {
            throw new IllegalArgumentException("Files and participantId must be provided");
        }
        try {
            Map<String, String> headers = new ConcurrentHashMap<>();
            headers.put(CommonConstants.CONTENT_TYPE, CommonConstants.MULTIPART_FORM_DATA);
            headers.put(CommonConstants.AUTHORIZATION, commonUtils.getToken());
            UriComponentsBuilder url = UriComponentsBuilder.fromUriString(
                    webConstants.getDmsIndividualDocumentUploadUrl());

            List<ChatMessageAttachment> chatMessageAttachments = files.stream().map(file -> {
                Map<String, Object> payload = new ConcurrentHashMap<>();
                Map<String, Object> dto = new HashMap<>();
                dto.put("userId", participantId);
                dto.put("fileName", file.getOriginalFilename());
                dto.put("remarks",
                        "multimedia_message_" + participantId + "_" + new Timestamp(System.currentTimeMillis()));
                dto.put("documentType", "CHAT_MESSAGE_ATTACHMENT");

                payload.put("file", file);
                payload.put("dto", dto);

                ResponseEntity<?> dmsResponse = restService.cmsRestCall(url.toUriString(), payload, headers,
                        HttpMethod.POST, 1L);
                if (dmsResponse.getStatusCode().is2xxSuccessful() && !ObjectUtils.isEmpty(dmsResponse.getBody())) {
                    JSONObject dmsResponseBody = new JSONObject(dmsResponse.getBody().toString());
                    String documentUrl = dmsResponseBody.optString("documentUrl");
                    ChatMessageAttachment chatMessageAttachment = new ChatMessageAttachment();
                    chatMessageAttachment.setFilePath(documentUrl);
                    chatMessageAttachment.setFileName(file.getOriginalFilename());
                    chatMessageAttachment.setAttachmentType(
                            commonUtils.validateAttachmentType(Objects.requireNonNull(file.getContentType())));
                    return chatMessageAttachment;
                }
                return null;
            }).filter(Objects::nonNull).toList();

            return ResponseEntity.ok(chatMessageAttachments);
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "MessageServiceImpl",
                    "Failed to send multimedia message",
                    "sendMultimediaMesssage",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }
}
