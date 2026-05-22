package com.nexus.cms.chat.service.implementations;

import com.nexus.cms.chat.entities.*;
import com.nexus.cms.chat.enums.ChatMessageStatus;
import com.nexus.cms.chat.model.ChatMessageRequestDto;
import com.nexus.cms.chat.repositories.ChatConversationRepo;
import com.nexus.cms.chat.repositories.ChatMessageIndividualStatusRepo;
import com.nexus.cms.chat.repositories.ChatMessageRepo;
import com.nexus.cms.chat.service.interfaces.MessageService;
import com.nexus.cms.exception.ResourceNotFoundException;
import com.nexus.cms.exception.ServiceLevelException;
import com.nexus.cms.util.CommonConstants;
import com.nexus.cms.util.CommonUtils;
import com.nexus.cms.util.RestService;
import com.nexus.cms.util.WebConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
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

    @Override
    public ResponseEntity<ChatMessage> sendMessage(ChatMessageRequestDto message, List<MultipartFile> files) {
        try {
            ChatConversation chatConversation = chatConversationRepo.findById(message.getChatConversationId()).orElseThrow(() -> new ResourceNotFoundException(
                    "ChatConversation",
                    "id",
                    message.getChatConversationId()
            ));
            ChatConversationParticipant chatConversationParticipant = chatConversation.getChatConversationParticipants().stream().filter(participant -> participant.getParticipantId().equals(message.getParticipantId())).findFirst().orElseThrow(() -> new ResourceNotFoundException(
                    "ChatConversationParticipant",
                    "userId",
                    message.getParticipantId()
            ));

            ChatMessage chatMessage = modelMapper.map(message, ChatMessage.class);
            chatMessage.setChatMessageStatus(ChatMessageStatus.SENT);
            chatMessage.setChatConversationParticipant(chatConversationParticipant);
            chatMessage.setChatConversation(chatConversation);


            chatMessage = chatMessageRepo.save(chatMessage);
            ChatMessage finalChatMessage1 = chatMessage;
            List<ChatMessageIndividualStatus> chatMessageIndividualStatuses = chatConversation.getChatConversationParticipants().stream().map(participant -> {
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
                message.getChatMessageAttachmentList().forEach(attachment -> attachment.setChatMessage(finalChatMessage));
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
                    dto.put("remarks", "conversation_" + chatConversation.getChatConversationId() + "_message_" + finalChatMessage.getChatMessageId() + "_" + new Timestamp(System.currentTimeMillis()));
                    dto.put("documentType", "CHAT_MESSAGE_ATTACHMENT");

                    payload.put("file", file);
                    payload.put("dto", dto);

                    ResponseEntity<?> dmsResponse = restService.cmsRestCall(url.toUriString(), payload, headers, HttpMethod.POST, chatConversation.getOrgId());
                    if (dmsResponse.getStatusCode().is2xxSuccessful() && !ObjectUtils.isEmpty(dmsResponse.getBody())) {
                        JSONObject dmsResponseBody = new JSONObject(dmsResponse.getBody().toString());
                        String documentUrl = dmsResponseBody.optString("documentUrl");
                        ChatMessageAttachment chatMessageAttachment = new ChatMessageAttachment();
                        chatMessageAttachment.setChatMessage(finalChatMessage);
                        chatMessageAttachment.setFilePath(documentUrl);
                        chatMessageAttachment.setFileName(file.getOriginalFilename());
                        chatMessageAttachment.setAttachmentType(commonUtils.validateAttachmentType(Objects.requireNonNull(file.getContentType())));
                        attachments.add(chatMessageAttachment);
                    }
                });

            }
            chatMessage.setChatMessageAttachmentList(attachments);
            chatMessage = chatMessageRepo.save(chatMessage);
            return ResponseEntity.ok(chatMessage);
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "MessageServiceImpl",
                    "Failed to send message",
                    "sendMesssage",
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> editMessage(ChatMessageRequestDto message) {
        try {
            ChatMessage chatMessage = chatMessageRepo.findById(message.getChatMessageId()).orElseThrow(() -> new ResourceNotFoundException(
                    "ChatMessage",
                    "id",
                    message.getChatMessageId()
            ));
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
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> sendMultimediaMessage(List<MultipartFile> files, Long participantId) {
        if (ObjectUtils.isEmpty(files) || ObjectUtils.isEmpty(participantId)){
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
                dto.put("remarks", "multimedia_message_" + participantId + "_" + new Timestamp(System.currentTimeMillis()));
                dto.put("documentType", "CHAT_MESSAGE_ATTACHMENT");

                payload.put("file", file);
                payload.put("dto", dto);

                ResponseEntity<?> dmsResponse = restService.cmsRestCall(url.toUriString(), payload, headers, HttpMethod.POST, 1L);
                if (dmsResponse.getStatusCode().is2xxSuccessful() && !ObjectUtils.isEmpty(dmsResponse.getBody())) {
                    JSONObject dmsResponseBody = new JSONObject(dmsResponse.getBody().toString());
                    String documentUrl = dmsResponseBody.optString("documentUrl");
                    ChatMessageAttachment chatMessageAttachment = new ChatMessageAttachment();
                    chatMessageAttachment.setFilePath(documentUrl);
                    chatMessageAttachment.setFileName(file.getOriginalFilename());
                    chatMessageAttachment.setAttachmentType(commonUtils.validateAttachmentType(Objects.requireNonNull(file.getContentType())));
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
                    e.getMessage()
            );
        }
    }
}
