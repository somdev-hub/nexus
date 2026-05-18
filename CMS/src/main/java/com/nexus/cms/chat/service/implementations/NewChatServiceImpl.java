package com.nexus.cms.chat.service.implementations;

import com.nexus.cms.chat.entities.ChatConversation;
import com.nexus.cms.chat.entities.ChatConversationParticipant;
import com.nexus.cms.chat.entities.ChatMessage;
import com.nexus.cms.chat.entities.ChatMessageAttachment;
import com.nexus.cms.chat.enums.ChatConversationType;
import com.nexus.cms.chat.enums.ChatMessageType;
import com.nexus.cms.chat.enums.ChatParticipantStatus;
import com.nexus.cms.chat.model.ChatConversationDetailedSummary;
import com.nexus.cms.chat.model.ChatConversationQuickResponseDto;
import com.nexus.cms.chat.model.ConversationMessagesDto;
import com.nexus.cms.chat.model.ConversationRequestDto;
import com.nexus.cms.chat.repositories.ChatConversationRepo;
import com.nexus.cms.chat.repositories.ChatMessageRepo;
import com.nexus.cms.chat.service.interfaces.NewChatService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewChatServiceImpl implements NewChatService {

    private final ChatConversationRepo chatConversationRepo;
    private final ChatMessageRepo chatMessageRepo;
    private final WebConstants webConstants;
    private final RestService restService;
    private final ModelMapper modelMapper;
    private final CommonUtils commonUtils;

    @Transactional
    @Override
    public ResponseEntity<?> createChatConversation(ConversationRequestDto request, MultipartFile chatConversationAvatar) {
        try {

            List<ChatConversationParticipant> chatConversationParticipants = new ArrayList<>(request.getChatConversationParticipants().stream().map(chatConversationParticipantDto -> {
                ChatConversationParticipant conversationParticipant = modelMapper.map(chatConversationParticipantDto, ChatConversationParticipant.class);
                conversationParticipant.setChatParticipantStatus(ChatParticipantStatus.JOINED);
                return conversationParticipant;
            }).toList());

            ChatConversation chatConversation = modelMapper.map(request, ChatConversation.class);
            chatConversationParticipants.forEach(participant -> participant.setChatConversation(chatConversation));
            chatConversation.setChatConversationParticipants(chatConversationParticipants);
            chatConversation.setTotalParticipants((long) chatConversationParticipants.size());
            chatConversation.setTotalMessages(0L);
            chatConversation.setLastModifiedBy(chatConversationParticipants.stream().filter(ChatConversationParticipant::getIsChatCreator).findFirst().orElseThrow().getParticipantId());

            if (!ObjectUtils.isEmpty(chatConversationAvatar)) {
                String contentType = chatConversationAvatar.getContentType();
                if (ObjectUtils.isEmpty(contentType)) {
                    throw new ServiceLevelException(
                            "NewChatServiceImpl",
                            "Unsupported file type for chat conversation avatar",
                            "createChatConversation",
                            "UnsupportedFileTypeException",
                            "Only JPEG and PNG images are supported for chat conversation avatars."
                    );
                }
                String fileExtension = switch (contentType) {
                    case "image/jpeg" -> "jpg";
                    case "image/png" -> "png";
                    default -> throw new ServiceLevelException(
                            "NewChatServiceImpl",
                            "Unsupported file type for chat conversation avatar",
                            "createChatConversation",
                            "UnsupportedFileTypeException",
                            "Only JPEG and PNG images are supported for chat conversation avatars."
                    );
                };
                Map<String, Object> payload = new ConcurrentHashMap<>();
                Map<String, Object> dto = new HashMap<>();
                dto.put("orgId", request.getOrgId());
                dto.put("fileName", request.getChatConversationName() + "_avatar." + fileExtension);
                dto.put("remarks", "Chat Conversation Avatar");
                dto.put("documentType", "PROFILE_IMAGE");

                payload.put("file", chatConversationAvatar);
                payload.put("dto", dto);

                Map<String, String> headers = new HashMap<>();
                headers.put(CommonConstants.CONTENT_TYPE, CommonConstants.MULTIPART_FORM_DATA);
                headers.put(CommonConstants.AUTHORIZATION, commonUtils.getToken());

                UriComponentsBuilder url = UriComponentsBuilder.fromUriString(
                        webConstants.getDmsOrgDocumentUploadUrl());

                ResponseEntity<?> dmsResponse = restService.cmsRestCall(url.toUriString(), payload, headers, HttpMethod.POST, request.getOrgId());
                if (dmsResponse.getStatusCode().is2xxSuccessful() && !ObjectUtils.isEmpty(dmsResponse.getBody())) {
                    JSONObject dmsResponseBody = new JSONObject(dmsResponse.getBody().toString());
                    chatConversation.setChatConversationAvatar(dmsResponseBody.optString("documentUrl"));
                }
            }

            ChatConversation savedChatConversation = chatConversationRepo.save(chatConversation);

            return ResponseEntity.ok(savedChatConversation);

        } catch (Exception e) {
            throw new ServiceLevelException(
                    "NewChatServiceImpl",
                    "Failed to create chat conversation",
                    "createChatConversation",
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> getChatConversationMessages(Long participantId) {
        try {
            List<ChatConversation> chatConversations = chatConversationRepo.findByParticipantId(participantId);
            List<ChatConversationQuickResponseDto> chatConversationQuickResponseDtos = chatConversations.stream().map(conversation -> {
                ChatConversationQuickResponseDto chatConversationQuickResponseDto = modelMapper.map(conversation, ChatConversationQuickResponseDto.class);
                ChatConversationParticipant chatConversationParticipant = conversation.getChatConversationParticipants().stream().filter(participant -> participant.getParticipantId().equals(participantId)).findFirst().orElseThrow(() -> new ResourceNotFoundException("Participant", "participantId", participantId));
                Timestamp lastRead = chatConversationParticipant.getLastRead();
                long count = conversation.getChatMessages().stream().filter(message -> message.getSentAt().after(lastRead)).count();
                // get latest message
                ChatMessage latestMessage = conversation.getChatMessages().isEmpty() ? null : conversation.getChatMessages().getLast();

                if (!ObjectUtils.isEmpty(latestMessage)) {
                    if (latestMessage.getChatMessageType().equals(ChatMessageType.TEXT)) {
                        chatConversationQuickResponseDto.setLastMessage(latestMessage.getChatMessageText());
                    } else {
                        chatConversationQuickResponseDto.setLastMessage(latestMessage.getChatMessageType().name());
                    }
                    chatConversationQuickResponseDto.setLastMessageAt(latestMessage.getSentAt());
                    chatConversationQuickResponseDto.setLastMessageSenderId(latestMessage.getChatConversationParticipant().getParticipantId());
                    chatConversationQuickResponseDto.setLastMessageSenderName(latestMessage.getChatConversationParticipant().getParticipantName());
                }
                chatConversationQuickResponseDto.setUnreadCount(count);

                if (conversation.getChatConversationType().equals(ChatConversationType.DIRECT)) {
                    ChatConversationParticipant otherParticipant = conversation.getChatConversationParticipants().stream().filter(participant -> !participant.equals(chatConversationParticipant)).findFirst().orElseThrow(() -> new RuntimeException("Other participant not found"));
                    chatConversationQuickResponseDto.setOtherParticipantName(otherParticipant.getParticipantName());
                    chatConversationQuickResponseDto.setOtherParticipantAvatar(otherParticipant.getParticipantAvatar());
                    chatConversationQuickResponseDto.setOtherParticipantId(otherParticipant.getParticipantId());
                }

                return chatConversationQuickResponseDto;
            }).toList();

            if (chatConversationQuickResponseDtos.isEmpty()) {
                return new ResponseEntity<>(chatConversationQuickResponseDtos, HttpStatus.NO_CONTENT);
            }

            return ResponseEntity.ok(chatConversationQuickResponseDtos);
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "NewChatServiceImpl",
                    "Failed to retrieve chat conversations for participant",
                    "getChatConversation",
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> getChatConversationMessages(Long conversationId, Long participantId, Long beforeId, Long limit) {
        try {
            Pageable pageable = PageRequest.of(0, limit.intValue());
            List<ChatMessage> chatMessages =
                    (beforeId == null) ?
                            chatMessageRepo.findLatestMessages(conversationId, pageable) :
                            chatMessageRepo.findMessagesBefore(conversationId, pageable, beforeId);

            if (ObjectUtils.isEmpty(chatMessages) || chatMessages.isEmpty()){
                Map<String, Object> result = new HashMap<>();
                result.put("messages", new ArrayList<>());
                result.put("hasMore", false);
                result.put("nextCursor", null);
                return new ResponseEntity<>(result, HttpStatus.NO_CONTENT);
            }

            boolean hasMore = chatMessages.size() >= limit;
            Long nextCursor = hasMore ? chatMessages.getFirst().getChatMessageId() : null;
            List<ConversationMessagesDto> conversationMessagesDtos = chatMessages.stream().map(message -> {
                ConversationMessagesDto conversationMessagesDto = modelMapper.map(message, ConversationMessagesDto.class);
                List<ConversationMessagesDto.MessageSeenBy> messageSeenByList = message
                        .getChatConversation()
                        .getChatConversationParticipants()
                        .stream()
                        .filter(participant -> participant.getLastRead() != null && participant.getLastRead().after(message.getSentAt()))
                        .map(participant -> modelMapper.map(participant, ConversationMessagesDto.MessageSeenBy.class))
                        .toList();
                conversationMessagesDto.setMessageSeenByList(messageSeenByList);
                return conversationMessagesDto;
            }).toList();

            Map<String, Object> result = new ConcurrentHashMap<>();
            result.put("messages", conversationMessagesDtos);
            result.put("hasMore", hasMore);
            result.put("nextCursor", nextCursor);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            throw new ServiceLevelException(
                    "NewChatServiceImpl",
                    "Failed to retrieve chat conversation messages",
                    "getChatConversation",
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> getChatConversation(Long conversationId, Long participantId) {
        try {
            ChatConversation chatConversation = chatConversationRepo.findById(conversationId).orElseThrow(() -> new ResourceNotFoundException("ChatConversation", "conversationId", conversationId));
            ChatConversationDetailedSummary chatConversationDetailedSummary = modelMapper.map(chatConversation, ChatConversationDetailedSummary.class);
            chatConversation.getChatMessages().forEach(message -> {
                List<ChatMessageAttachment> chatMessageAttachmentList = message.getChatMessageAttachmentList();
                List<ChatMessageAttachment> imageAndVideoAttachments = chatMessageAttachmentList.stream().filter(attachment -> attachment.getAttachmentType() != null &&
                        CommonConstants.IMAGE_AND_VIDEO_ATTACHMENT_TYPES.contains(attachment.getAttachmentType())).toList();
                List<ChatMessageAttachment> fileAttachments = chatMessageAttachmentList.stream().filter(attachment -> attachment.getAttachmentType() != null &&
                        CommonConstants.FILE_ATTACHMENT_TYPES.contains(attachment.getAttachmentType())).toList();
                chatConversationDetailedSummary.getImageAndVideoAttachments().addAll(imageAndVideoAttachments);
                chatConversationDetailedSummary.getFileAttachments().addAll(fileAttachments);
            });
            return ResponseEntity.ok(chatConversationDetailedSummary);
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "NewChatServiceImpl",
                    "Failed to retrieve chat conversation details",
                    "getChatConversation",
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> updateChatConversation(ConversationRequestDto request, MultipartFile chatConversationAvatar) {
        try {
            ChatConversation chatConversation = chatConversationRepo.findById(request.getChatConversationId()).orElseThrow(() -> new ResourceNotFoundException("ChatConversation", "conversationId", request.getChatConversationId()));
            modelMapper.map(request, chatConversation);
            if (!ObjectUtils.isEmpty(chatConversationAvatar)) {
                String contentType = chatConversationAvatar.getContentType();
                if (ObjectUtils.isEmpty(contentType)) {
                    throw new ServiceLevelException(
                            "NewChatServiceImpl",
                            "Unsupported file type for chat conversation avatar",
                            "updateChatConversation",
                            "UnsupportedFileTypeException",
                            "Only JPEG and PNG images are supported for chat conversation avatars."
                    );
                }
                String fileExtension = switch (contentType) {
                    case "image/jpeg" -> "jpg";
                    case "image/png" -> "png";
                    default -> throw new ServiceLevelException(
                            "NewChatServiceImpl",
                            "Unsupported file type for chat conversation avatar",
                            "updateChatConversation",
                            "UnsupportedFileTypeException",
                            "Only JPEG and PNG images are supported for chat conversation avatars."
                    );
                };
                Map<String, Object> payload = new ConcurrentHashMap<>();
                Map<String, Object> dto = new HashMap<>();
                dto.put("orgId", request.getOrgId());
                dto.put("fileName", request.getChatConversationName() + "_avatar." + fileExtension);
                dto.put("remarks", "Chat Conversation Avatar");
                dto.put("documentType", "PROFILE_IMAGE");

                payload.put("file", chatConversationAvatar);
                payload.put("dto", dto);

                Map<String, String> headers = new HashMap<>();
                headers.put(CommonConstants.CONTENT_TYPE, CommonConstants.MULTIPART_FORM_DATA);
                headers.put(CommonConstants.AUTHORIZATION, commonUtils.getToken());

                UriComponentsBuilder url = UriComponentsBuilder.fromUriString(
                        webConstants.getDmsOrgDocumentUploadUrl());

                ResponseEntity<?> dmsResponse = restService.cmsRestCall(url.toUriString(), payload, headers, HttpMethod.POST, request.getOrgId());
                if (dmsResponse.getStatusCode().is2xxSuccessful() && !ObjectUtils.isEmpty(dmsResponse.getBody())) {
                    JSONObject dmsResponseBody = new JSONObject(dmsResponse.getBody().toString());
                    chatConversation.setChatConversationAvatar(dmsResponseBody.optString("documentUrl"));
                }
            }
            ChatConversation updatedChatConversation = chatConversationRepo.save(chatConversation);
            return ResponseEntity.ok(updatedChatConversation);
        } catch (Exception e) {
            throw new ServiceLevelException(
                    "NewChatServiceImpl",
                    "Failed to update chat conversation",
                    "updateChatConversation",
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
        }
    }
}
