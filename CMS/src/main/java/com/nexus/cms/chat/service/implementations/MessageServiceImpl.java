package com.nexus.cms.chat.service.implementations;

import com.nexus.cms.chat.entities.ChatConversation;
import com.nexus.cms.chat.entities.ChatConversationParticipant;
import com.nexus.cms.chat.entities.ChatMessage;
import com.nexus.cms.chat.entities.ChatMessageAttachment;
import com.nexus.cms.chat.enums.ChatMessageStatus;
import com.nexus.cms.chat.model.ChatMessageRequestDto;
import com.nexus.cms.chat.repositories.ChatConversationRepo;
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

    @Override
    public ResponseEntity<?> sendMessage(ChatMessageRequestDto message, List<MultipartFile> files) {
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

            if (!ObjectUtils.isEmpty(files)) {
                Map<String, String> headers = new ConcurrentHashMap<>();
                headers.put(CommonConstants.CONTENT_TYPE, CommonConstants.MULTIPART_FORM_DATA);
                headers.put(CommonConstants.AUTHORIZATION, commonUtils.getToken());
                UriComponentsBuilder url = UriComponentsBuilder.fromUriString(
                        webConstants.getDmsIndividualDocumentUploadUrl());

                ChatMessage finalChatMessage = chatMessage;
                List<ChatMessageAttachment> attachments = new ArrayList<>();
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

                chatMessage.setChatMessageAttachmentList(attachments);
            }
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
}
