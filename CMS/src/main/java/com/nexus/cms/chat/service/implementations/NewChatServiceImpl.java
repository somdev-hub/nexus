package com.nexus.cms.chat.service.implementations;

import com.nexus.cms.chat.entities.*;
import com.nexus.cms.chat.enums.ChatConversationType;
import com.nexus.cms.chat.enums.ChatMessageStatus;
import com.nexus.cms.chat.enums.ChatMessageType;
import com.nexus.cms.chat.enums.ChatParticipantStatus;
import com.nexus.cms.chat.model.ChatConversationDetailedSummary;
import com.nexus.cms.chat.model.ChatConversationQuickResponseDto;
import com.nexus.cms.chat.model.ConversationMessagesDto;
import com.nexus.cms.chat.model.ChatConversationParticipantDto;
import com.nexus.cms.chat.model.ConversationRequestDto;
import com.nexus.cms.chat.repositories.ChatConversationParticipantRepo;
import com.nexus.cms.chat.repositories.ChatConversationRepo;
import com.nexus.cms.chat.repositories.ChatMessageIndividualStatusRepo;
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
import org.springframework.scheduling.annotation.Async;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
        private final ChatConversationParticipantRepo chatConversationParticipantRepo;
        private final ChatMessageIndividualStatusRepo chatMessageIndividualStatusRepo;

        @Transactional(timeout = 30)
        @Override
        public ResponseEntity<?> createChatConversation(ConversationRequestDto request,
                        MultipartFile chatConversationAvatar) {
                try {
                        // Validate request before processing
                        validateConversationCreationRequest(request);

                        // Idempotent DIRECT conversation creation: return existing if present
                        if (ChatConversationType.DIRECT.equals(request.getChatConversationType())
                                        && request.getChatConversationParticipants() != null
                                        && request.getChatConversationParticipants().size() == 2) {
                                Long p1 = request.getChatConversationParticipants().get(0).getParticipantId();
                                Long p2 = request.getChatConversationParticipants().get(1).getParticipantId();
                                Optional<ChatConversation> existing = chatConversationRepo.findDirectConversationBetween(p1, p2);
                                if (existing.isPresent()) {
                                        log.info("Returning existing DIRECT conversation between {} and {}", p1, p2);
                                        return ResponseEntity.ok(existing.get());
                                }
                        }

                        List<ChatConversationParticipant> chatConversationParticipants = new ArrayList<>(
                                        request.getChatConversationParticipants().stream()
                                                        .map(chatConversationParticipantDto -> {
                                                                ChatConversationParticipant conversationParticipant = new ChatConversationParticipant();
                                                                conversationParticipant.setParticipantId(chatConversationParticipantDto.getParticipantId());
                                                                conversationParticipant.setParticipantName(chatConversationParticipantDto.getParticipantName());
                                                                conversationParticipant.setParticipantEmail(chatConversationParticipantDto.getParticipantEmail());
                                                                conversationParticipant.setParticipantMob(chatConversationParticipantDto.getParticipantMob());
                                                                conversationParticipant.setParticipantRole(chatConversationParticipantDto.getParticipantRole());
                                                                conversationParticipant.setParticipantAvatar(chatConversationParticipantDto.getParticipantAvatar());
                                                                conversationParticipant.setIsChatCreator(chatConversationParticipantDto.getIsChatCreator());
                                                                conversationParticipant.setChatParticipantType(chatConversationParticipantDto.getChatParticipantType());
                                                                conversationParticipant.setChatParticipantStatus(ChatParticipantStatus.JOINED);
                                                                conversationParticipant.setOrgId(chatConversationParticipantDto.getOrgId());
                                                                conversationParticipant.setOrgName(chatConversationParticipantDto.getOrgName());
                                                                conversationParticipant.setOrgType(chatConversationParticipantDto.getOrgType());
                                                                return conversationParticipant;
                                                        }).toList());

                        ChatConversation chatConversation = new ChatConversation();
                        chatConversation.setChatConversationName(request.getChatConversationName());
                        chatConversation.setChatConversationDescription(request.getChatConversationDescription());
                        chatConversation.setChatConversationType(request.getChatConversationType());
                        chatConversation.setOrgId(request.getOrgId());
                        chatConversation.setIsInterOrg(request.getIsInterOrg());
                        chatConversation.setIsActive(request.getIsActive());
                        chatConversationParticipants
                                        .forEach(participant -> participant.setChatConversation(chatConversation));
                        chatConversation.setChatConversationParticipants(chatConversationParticipants);
                        chatConversation.setTotalParticipants((long) chatConversationParticipants.size());
                        chatConversation.setTotalMessages(0L);

                        // Set inter-org fields
                        if (request.getIsInterOrg() != null) {
                            chatConversation.setIsInterOrg(request.getIsInterOrg());
                        } else {
                            // Auto-detect: if participants span multiple orgs
                            java.util.Set<Long> orgIds = new java.util.HashSet<>();
                            for (var p : request.getChatConversationParticipants()) {
                                if (p.getOrgId() != null) {
                                    orgIds.add(p.getOrgId());
                                }
                            }
                            chatConversation.setIsInterOrg(orgIds.size() > 1);
                        }

                        // Set participant org IDs as JSON
                        java.util.List<Long> participantOrgIds = new java.util.ArrayList<>();
                        for (var p : request.getChatConversationParticipants()) {
                            if (p.getOrgId() != null && !participantOrgIds.contains(p.getOrgId())) {
                                participantOrgIds.add(p.getOrgId());
                            }
                        }
                        try {
                            chatConversation.setParticipantOrgIds(
                                    new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(participantOrgIds));
                        } catch (Exception e) {
                            log.warn("Failed to serialize participant org IDs", e);
                        }

                        // Set orgId on each participant
                        for (int i = 0; i < chatConversationParticipants.size(); i++) {
                            var dto = request.getChatConversationParticipants().get(i);
                            var participant = chatConversationParticipants.get(i);
                            if (dto.getOrgId() != null) {
                                participant.setOrgId(dto.getOrgId());
                            }
                            if (dto.getOrgName() != null) {
                                participant.setOrgName(dto.getOrgName());
                            }
                            if (dto.getOrgType() != null) {
                                participant.setOrgType(dto.getOrgType());
                            }
                        }

                        ChatConversationParticipant creator = chatConversationParticipants.stream()
                                        .filter(ChatConversationParticipant::getIsChatCreator)
                                        .findFirst()
                                        .orElseThrow(() -> new ServiceLevelException(
                                                        "NewChatServiceImpl",
                                                        "No creator specified for conversation",
                                                        "createChatConversation",
                                                        "ValidationException",
                                                        "At least one participant must be marked as chat creator"));

                        chatConversation.setLastModifiedBy(creator.getParticipantId());

                        if (!ObjectUtils.isEmpty(chatConversationAvatar)) {
                                String contentType = chatConversationAvatar.getContentType();
                                if (ObjectUtils.isEmpty(contentType)) {
                                        throw new ServiceLevelException(
                                                        "NewChatServiceImpl",
                                                        "Unsupported file type for chat conversation avatar",
                                                        "createChatConversation",
                                                        "UnsupportedFileTypeException",
                                                        "Only JPEG and PNG images are supported for chat conversation avatars.");
                                }
                                String fileExtension = switch (contentType) {
                                        case "image/jpeg" -> "jpg";
                                        case "image/png" -> "png";
                                        default -> throw new ServiceLevelException(
                                                        "NewChatServiceImpl",
                                                        "Unsupported file type for chat conversation avatar",
                                                        "createChatConversation",
                                                        "UnsupportedFileTypeException",
                                                        "Only JPEG and PNG images are supported for chat conversation avatars.");
                                };
                                Map<String, Object> payload = new ConcurrentHashMap<>();
                                Map<String, Object> dto = new HashMap<>();
                                Long dmsOrgId = request.getOrgId();
                                if (dmsOrgId == null && !request.getChatConversationParticipants().isEmpty()) {
                                    dmsOrgId = request.getChatConversationParticipants().get(0).getOrgId();
                                }
                                dto.put("orgId", dmsOrgId);
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

                                ResponseEntity<?> dmsResponse = restService.cmsRestCall(url.toUriString(), payload,
                                                headers,
                                                HttpMethod.POST, dmsOrgId);
                                if (dmsResponse.getStatusCode().is2xxSuccessful()
                                                && !ObjectUtils.isEmpty(dmsResponse.getBody())) {
                                        JSONObject dmsResponseBody = new JSONObject(dmsResponse.getBody().toString());
                                        chatConversation.setChatConversationAvatar(
                                                        dmsResponseBody.optString("documentUrl"));
                                }
                        }

                        ChatConversation savedChatConversation = chatConversationRepo.save(chatConversation);

                        return ResponseEntity.ok(savedChatConversation);

                } catch (Exception e) {
                        log.error("Failed to create chat conversation", e);
                        throw new ServiceLevelException(
                                        "NewChatServiceImpl",
                                        "Failed to create chat conversation: " + e.getMessage(),
                                        "createChatConversation",
                                        e.getClass().getSimpleName(),
                                        e.getMessage());
                }
        }

        @Override
        @Transactional
        public ResponseEntity<?> getChatConversationMessages(Long participantId) {
                try {
                        List<ChatConversation> chatConversations = chatConversationRepo
                                        .findByParticipantId(participantId);
                        List<ChatConversationQuickResponseDto> chatConversationQuickResponseDtos = chatConversations
                                        .stream()
                                        .map(conversation -> extracted(participantId, conversation)).toList();

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
                                        e.getMessage());
                }
        }

        private ChatConversationQuickResponseDto extracted(Long participantId, ChatConversation conversation) {
                try {
                ChatConversationQuickResponseDto chatConversationQuickResponseDto = modelMapper
                                .map(conversation,
                                                ChatConversationQuickResponseDto.class);
                ChatConversationParticipant chatConversationParticipant = conversation
                                .getChatConversationParticipants().stream()
                                .filter(participant -> participant.getParticipantId()
                                                .equals(participantId))
                                .findFirst()
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Participant", "participantId",
                                                participantId));
                // Fix N+1: Use direct database query instead of loading all messages
                Long lastReadMessageId = chatConversationParticipant.getLastMessageId();
                long count = chatMessageRepo.countUnreadMessagesByCursor(
                                conversation.getChatConversationId(),
                                lastReadMessageId == null ? 0L : lastReadMessageId,
                                participantId);

                // Use denormalized last-message fields (no lazy collection access)
                if (!ObjectUtils.isEmpty(conversation.getLastMessageText())) {
                        chatConversationQuickResponseDto.setLastMessage(conversation.getLastMessageText());
                } else {
                        // Backfill legacy conversations that have no denormalized last-message metadata
                        try {
                                org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 1);
                                java.util.List<ChatMessage> latest = chatMessageRepo.findLatestMessages(conversation.getChatConversationId(), pageable);
                                if (latest != null && !latest.isEmpty()) {
                                        ChatMessage lm = latest.get(0);
                                        String text = (lm.getChatMessageType() == ChatMessageType.TEXT)
                                                        ? lm.getChatMessageText()
                                                        : (lm.getChatMessageType() != null ? lm.getChatMessageType().name() : "Message");
                                        chatConversationQuickResponseDto.setLastMessage(text);
                                        chatConversationQuickResponseDto.setLastMessageAt(lm.getSentAt());
                                        if (lm.getChatConversationParticipant() != null) {
                                                chatConversationQuickResponseDto.setLastMessageSenderId(lm.getChatConversationParticipant().getParticipantId());
                                                chatConversationQuickResponseDto.setLastMessageSenderName(lm.getChatConversationParticipant().getParticipantName());
                                        }
                                        // Self-heal: persist denormalized metadata for future calls
                                        conversation.setLastMessageText(text);
                                        conversation.setLastMessageAt(lm.getSentAt());
                                        if (lm.getChatConversationParticipant() != null) {
                                                conversation.setLastMessageSenderId(lm.getChatConversationParticipant().getParticipantId());
                                                conversation.setLastMessageSenderName(lm.getChatConversationParticipant().getParticipantName());
                                        }
                                        if (conversation.getTotalMessages() == null) {
                                                conversation.setTotalMessages(1L);
                                        }
                                        chatConversationRepo.save(conversation);
                                } else {
                                        chatConversationQuickResponseDto.setLastMessage("Start a conversation");
                                }
                        } catch (Exception e) {
                                log.warn("Failed to backfill last message for conversation {}", conversation.getChatConversationId(), e);
                                chatConversationQuickResponseDto.setLastMessage("Start a conversation");
                        }
                }
                if (!ObjectUtils.isEmpty(conversation.getLastMessageText())) {
                        chatConversationQuickResponseDto.setLastMessageAt(conversation.getLastMessageAt());
                        chatConversationQuickResponseDto.setLastMessageSenderId(conversation.getLastMessageSenderId());
                        chatConversationQuickResponseDto.setLastMessageSenderName(conversation.getLastMessageSenderName());
                }
                chatConversationQuickResponseDto.setUnreadCount(count);

                if (conversation.getChatConversationType()
                                .equals(ChatConversationType.DIRECT)) {
                        Optional<ChatConversationParticipant> otherParticipantOpt = conversation
                                        .getChatConversationParticipants().stream()
                                        .filter(participant -> !participant.getParticipantId()
                                                        .equals(chatConversationParticipant.getParticipantId()))
                                        .findFirst();
                        if (otherParticipantOpt.isPresent()) {
                                ChatConversationParticipant otherParticipant = otherParticipantOpt.get();
                                chatConversationQuickResponseDto
                                                .setOtherParticipantName(otherParticipant
                                                                .getParticipantName());
                                chatConversationQuickResponseDto
                                                .setOtherParticipantAvatar(otherParticipant
                                                                .getParticipantAvatar());
                                chatConversationQuickResponseDto.setOtherParticipantId(
                                                otherParticipant.getParticipantId());
                        } else {
                                log.warn("No other participant found for DIRECT conversation {} and participant {}",
                                                conversation.getChatConversationId(), participantId);
                        }
                }

                return chatConversationQuickResponseDto;
                } catch (Exception e) {
                    log.error("Failed to extract conversation data for participant {}", participantId, e);
                    throw e;
                }
        }

        @Transactional(timeout = 30)
        @Override
        public ResponseEntity<?> getChatConversationMessages(Long conversationId, Long participantId, Long beforeId,
                        Long limit) {
                try {
                        ChatConversation chatConversation = chatConversationRepo.findById(conversationId).orElseThrow(
                                        () -> new ResourceNotFoundException("ChatConversation", "conversationId",
                                                        conversationId));
                        ChatConversationParticipant chatConversationParticipant = chatConversation
                                        .getChatConversationParticipants()
                                        .stream()
                                        .filter(participant -> participant.getParticipantId().equals(participantId))
                                        .findFirst()
                                        .orElseThrow(() -> new ResourceNotFoundException("Participant", "participantId",
                                                        participantId));

                        Pageable pageable = PageRequest.of(0, limit.intValue());
                        List<ChatMessage> chatMessages = (beforeId == null)
                                        ? chatMessageRepo.findLatestMessages(conversationId, pageable)
                                        : chatMessageRepo.findMessagesBefore(conversationId, pageable, beforeId);

                        if (ObjectUtils.isEmpty(chatMessages) || chatMessages.isEmpty()) {
                                Map<String, Object> result = new HashMap<>();
                                result.put("messages", new ArrayList<>());
                                result.put("hasMore", false);
                                result.put("nextCursor", null);
                                return new ResponseEntity<>(result, HttpStatus.NO_CONTENT);
                        }

                        // Phase 1: Update received statuses and track last read
                        updateReceivedStatuses(conversationId, participantId, chatMessages);

                        // Phase 3: Trigger async aggregate status update (non-blocking)
                        List<Long> fetchedMessageIds = chatMessages.stream()
                                        .map(ChatMessage::getChatMessageId).toList();
                        updateAggregateStatusAsync(conversationId, fetchedMessageIds);

                        // Update lastRead to the newest message's sentAt and lastMessageId
                        ChatMessage newestMessage = chatMessages.getFirst();
                        chatConversationParticipant.setLastRead(newestMessage.getSentAt());
                        chatConversationParticipant.setLastMessageId(newestMessage.getChatMessageId());
                        chatConversationParticipantRepo.save(chatConversationParticipant);

                        boolean hasMore = chatMessages.size() >= limit;
                        Long nextCursor = hasMore ? chatMessages.getFirst().getChatMessageId() : null;
                        List<ConversationMessagesDto> conversationMessagesDtos = chatMessages.stream().map(message -> {
                                ConversationMessagesDto conversationMessagesDto = modelMapper.map(message,
                                                ConversationMessagesDto.class);

                                // Map participant information
                                ChatConversationParticipant baseSender = message
                                                .getChatConversationParticipant();

                                // If sender participant is null (legacy messages), try to infer for DIRECT conversations
                                if (baseSender == null
                                                && chatConversation.getChatConversationType() == ChatConversationType.DIRECT
                                                && chatConversation.getChatConversationParticipants().size() == 2) {
                                        // For DIRECT conversations, sender is the participant who does NOT have an individual status entry
                                        // (statuses are created for recipients only)
                                        Set<Long> recipientParticipantIds = message.getChatMessageIndividualStatuses().stream()
                                                        .map(status -> status.getParticipant().getParticipantId())
                                                        .collect(Collectors.toSet());
                                        Optional<ChatConversationParticipant> inferredSender = chatConversation
                                                        .getChatConversationParticipants().stream()
                                                        .filter(p -> !recipientParticipantIds.contains(p.getParticipantId()))
                                                        .findFirst();
                                        if (inferredSender.isPresent()) {
                                                baseSender = inferredSender.get();
                                        }
                                }

                                final ChatConversationParticipant senderParticipant = baseSender;

                                if (senderParticipant != null) {
                                        ConversationMessagesDto.ParticipantInfo participantInfo = new ConversationMessagesDto.ParticipantInfo();
                                        participantInfo.setParticipantId(senderParticipant.getParticipantId());
                                        participantInfo.setParticipantName(senderParticipant.getParticipantName());
                                        participantInfo.setParticipantEmail(senderParticipant.getParticipantEmail());
                                        participantInfo.setParticipantAvatar(senderParticipant.getParticipantAvatar());
                                        conversationMessagesDto.setChatConversationParticipant(participantInfo);
                                }

                                List<ConversationMessagesDto.MessageSeenBy> messageSeenByList = message
                                                .getChatConversation()
                                                .getChatConversationParticipants()
                                                .stream()
                                                .filter(participant -> participant.getLastRead() != null
                                                                && participant.getLastRead().after(message.getSentAt())
                                                                && (senderParticipant == null
                                                                        || !participant.getParticipantId().equals(senderParticipant.getParticipantId())))
                                                .map(participant -> modelMapper.map(participant,
                                                                ConversationMessagesDto.MessageSeenBy.class))
                                                .toList();
                                conversationMessagesDto.setMessageSeenByList(messageSeenByList);
                                return conversationMessagesDto;
                        }).toList();

                        Map<String, Object> result = new HashMap<>();
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
                                        e.getMessage());
                }
        }

        @Override
        @Transactional(readOnly = true)
        public ResponseEntity<?> getChatConversation(Long conversationId, Long participantId) {
                try {
                        ChatConversation chatConversation = chatConversationRepo.findById(conversationId).orElseThrow(
                                        () -> new ResourceNotFoundException("ChatConversation", "conversationId",
                                                        conversationId));
                        ChatConversationDetailedSummary chatConversationDetailedSummary = modelMapper.map(
                                        chatConversation,
                                        ChatConversationDetailedSummary.class);
                        if (chatConversation.getChatMessages() != null) {
                                chatConversation.getChatMessages().forEach(message -> {
                                        List<ChatMessageAttachment> chatMessageAttachmentList = message
                                                        .getChatMessageAttachmentList();
                                        if (chatMessageAttachmentList == null) {
                                                return;
                                        }
                                        List<ChatMessageAttachment> imageAndVideoAttachments = chatMessageAttachmentList
                                                        .stream()
                                                        .filter(attachment -> attachment != null && attachment.getAttachmentType() != null &&
                                                                        CommonConstants.IMAGE_AND_VIDEO_ATTACHMENT_TYPES != null &&
                                                                        CommonConstants.IMAGE_AND_VIDEO_ATTACHMENT_TYPES
                                                                                        .contains(attachment
                                                                                                        .getAttachmentType()))
                                                        .toList();
                                        List<ChatMessageAttachment> fileAttachments = chatMessageAttachmentList.stream()
                                                        .filter(attachment -> attachment != null && attachment.getAttachmentType() != null &&
                                                                        CommonConstants.FILE_ATTACHMENT_TYPES != null &&
                                                                        CommonConstants.FILE_ATTACHMENT_TYPES.contains(
                                                                                        attachment.getAttachmentType()))
                                                        .toList();
                                        if (chatConversationDetailedSummary.getImageAndVideoAttachments() != null) {
                                                chatConversationDetailedSummary.getImageAndVideoAttachments()
                                                                .addAll(imageAndVideoAttachments);
                                        }
                                        if (chatConversationDetailedSummary.getFileAttachments() != null) {
                                                chatConversationDetailedSummary.getFileAttachments().addAll(fileAttachments);
                                        }
                                });
                        }
                        return ResponseEntity.ok(chatConversationDetailedSummary);
                } catch (Exception e) {
                        throw new ServiceLevelException(
                                        "NewChatServiceImpl",
                                        "Failed to retrieve chat conversation details",
                                        "getChatConversation",
                                        e.getClass().getSimpleName(),
                                        e.getMessage());
                }
        }

        @Override
        public ResponseEntity<?> updateChatConversation(ConversationRequestDto request,
                        MultipartFile chatConversationAvatar) {
                try {
                        ChatConversation chatConversation = chatConversationRepo
                                        .findById(request.getChatConversationId())
                                        .orElseThrow(() -> new ResourceNotFoundException("ChatConversation",
                                                        "conversationId",
                                                        request.getChatConversationId()));
                        // Only update provided fields; do not wipe existing values with nulls
                        if (request.getChatConversationName() != null) {
                                chatConversation.setChatConversationName(request.getChatConversationName());
                        }
                        if (request.getChatConversationDescription() != null) {
                                chatConversation.setChatConversationDescription(request.getChatConversationDescription());
                        }
                        if (request.getIsActive() != null) {
                                chatConversation.setIsActive(request.getIsActive());
                        }
                        if (!ObjectUtils.isEmpty(chatConversationAvatar)) {
                                String contentType = chatConversationAvatar.getContentType();
                                if (ObjectUtils.isEmpty(contentType)) {
                                        throw new ServiceLevelException(
                                                        "NewChatServiceImpl",
                                                        "Unsupported file type for chat conversation avatar",
                                                        "updateChatConversation",
                                                        "UnsupportedFileTypeException",
                                                        "Only JPEG and PNG images are supported for chat conversation avatars.");
                                }
                                String fileExtension = switch (contentType) {
                                        case "image/jpeg" -> "jpg";
                                        case "image/png" -> "png";
                                        default -> throw new ServiceLevelException(
                                                        "NewChatServiceImpl",
                                                        "Unsupported file type for chat conversation avatar",
                                                        "updateChatConversation",
                                                        "UnsupportedFileTypeException",
                                                        "Only JPEG and PNG images are supported for chat conversation avatars.");
                                };
                                Map<String, Object> payload = new ConcurrentHashMap<>();
                                Map<String, Object> dto = new HashMap<>();
                                Long dmsOrgId = request.getOrgId();
                                if (dmsOrgId == null && !request.getChatConversationParticipants().isEmpty()) {
                                    dmsOrgId = request.getChatConversationParticipants().get(0).getOrgId();
                                }
                                dto.put("orgId", dmsOrgId);
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

                                ResponseEntity<?> dmsResponse = restService.cmsRestCall(url.toUriString(), payload,
                                                headers,
                                                HttpMethod.POST, dmsOrgId);
                                if (dmsResponse.getStatusCode().is2xxSuccessful()
                                                && !ObjectUtils.isEmpty(dmsResponse.getBody())) {
                                        JSONObject dmsResponseBody = new JSONObject(dmsResponse.getBody().toString());
                                        chatConversation.setChatConversationAvatar(
                                                        dmsResponseBody.optString("documentUrl"));
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
                                        e.getMessage());
                }
        }

        @Override
        public ResponseEntity<?> viewConversation(Long participantId, Long conversationId) {
                try {
                        ChatConversation chatConversation = chatConversationRepo.findById(conversationId).orElseThrow(
                                        () -> new ResourceNotFoundException("ChatConversation", "conversationId",
                                                        conversationId));
                        ChatConversationParticipant chatConversationParticipant = chatConversation
                                        .getChatConversationParticipants()
                                        .stream()
                                        .filter(participant -> participant.getParticipantId().equals(participantId))
                                        .findFirst()
                                        .orElseThrow(() -> new ResourceNotFoundException("ChatConversationParticipant",
                                                        "participantId",
                                                        participantId));
                        chatConversationParticipant.setLastRead(new Timestamp(System.currentTimeMillis()));
                        chatConversationParticipantRepo.save(chatConversationParticipant);
                        return ResponseEntity.ok().build();
                } catch (Exception e) {
                        throw new ServiceLevelException(
                                        "NewChatServiceImpl",
                                        "Failed to mark conversation as viewed",
                                        "viewConversation",
                                        e.getClass().getSimpleName(),
                                        e.getMessage());
                }
        }

        @Override
        public ResponseEntity<ChatMessageIndividualStatus> markReceived(Long messageId, Long participantId) {
                try {
                        ChatMessage chatMessage = chatMessageRepo.findById(messageId)
                                        .orElseThrow(() -> new ResourceNotFoundException("ChatMessage", "messageId",
                                                        messageId));
                        ChatConversationParticipant chatConversationParticipant = chatConversationParticipantRepo
                                        .findByChatConversationIdAndParticipantId(
                                                        chatMessage.getChatConversation().getChatConversationId(),
                                                        participantId)
                                        .orElseThrow(() -> new ResourceNotFoundException("ChatConversationParticipant",
                                                        "participantId", participantId));
                        ChatMessageIndividualStatus individualStatus = chatMessageIndividualStatusRepo
                                        .findByChatMessageIdAndChatConversationParticipant(messageId,
                                                        chatConversationParticipant)
                                        .orElseGet(() -> {
                                                ChatMessageIndividualStatus newStatus = new ChatMessageIndividualStatus();
                                                newStatus.setChatMessage(chatMessage);
                                                newStatus.setParticipant(chatConversationParticipant);
                                                return newStatus;
                                        });
                        individualStatus.setStatus(ChatMessageStatus.RECEIVED);
                        chatMessageIndividualStatusRepo.save(individualStatus);
                        return ResponseEntity.ok(individualStatus);
                } catch (Exception e) {
                        throw new ServiceLevelException(
                                        "NewChatServiceImpl",
                                        "Failed to mark message as received",
                                        "markReceived",
                                        e.getClass().getSimpleName(),
                                        e.getMessage());
                }
        }

        /**
         * Phase 1: Batch update individual message statuses from SENT to RECEIVED
         * when participant fetches messages
         *
         * @param conversationId The conversation ID
         * @param participantId  The participant ID
         * @param chatMessages   The fetched chat messages
         */
        private void updateReceivedStatuses(Long conversationId, Long participantId, List<ChatMessage> chatMessages) {
                if (chatMessages.isEmpty()) {
                        return;
                }

                try {
                        // Extract message IDs from fetched messages
                        List<Long> messageIds = chatMessages.stream()
                                        .map(ChatMessage::getChatMessageId)
                                        .toList();

                        // Batch update: Set all SENT statuses to RECEIVED for this participant
                        chatMessageIndividualStatusRepo.batchUpdateStatusToReceived(participantId, messageIds,
                                        ChatMessageStatus.RECEIVED);

                        log.debug("Updated {} message statuses to RECEIVED for participant {} in conversation {}",
                                        messageIds.size(), participantId, conversationId);
                } catch (Exception e) {
                        log.error("Failed to update received statuses for participant {} in conversation {}",
                                        participantId, conversationId, e);
                        // Non-blocking: Don't fail the entire fetch operation if status update fails
                        // Status updates are eventually consistent
                }
        }

        /**
         * Phase 3: Calculate and update aggregate message status
         * Determines the max status across all individual participant statuses
         * If all participants RECEIVED → message RECEIVED
         * If any participant SENT → message SENT
         * Async: Non-blocking execution to keep fetch response fast
         * Eventually consistent: Updates may lag behind actual state by seconds
         *
         * @param conversationId The conversation ID
         */
        @Async
        public void updateAggregateStatusAsync(Long conversationId, List<Long> messageIds) {
                if (messageIds == null || messageIds.isEmpty()) {
                        return;
                }
                try {
                        // Get only the messages that need status recalculation
                        List<ChatMessage> chatMessages = chatMessageRepo.findAllById(messageIds);

                        // Update each message's aggregate status based on individual statuses
                        List<ChatMessage> updatedMessages = chatMessages.stream().map(message -> {
                                List<ChatMessageIndividualStatus> individualStatuses = message
                                                .getChatMessageIndividualStatuses();

                                if (individualStatuses.isEmpty()) {
                                        // No individual statuses yet (shouldn't happen, but handle gracefully)
                                        message.setChatMessageStatus(ChatMessageStatus.SENT);
                                        return message;
                                }

                                // Count status distribution
                                long sentCount = individualStatuses.stream()
                                                .filter(status -> status.getStatus().equals(ChatMessageStatus.SENT))
                                                .count();
                                long receivedCount = individualStatuses.stream()
                                                .filter(status -> status.getStatus().equals(ChatMessageStatus.RECEIVED))
                                                .count();

                                // Determine aggregate status: max across all participants
                                // RECEIVED (highest) > SENT (lowest)
                                if (receivedCount == individualStatuses.size()) {
                                        // All participants have received
                                        message.setChatMessageStatus(ChatMessageStatus.RECEIVED);
                                } else if (sentCount == individualStatuses.size()) {
                                        // All participants still in SENT state
                                        message.setChatMessageStatus(ChatMessageStatus.SENT);
                                } else {
                                        // Mixed: Some received, some not
                                        // Use PARTIALLY_RECEIVED to indicate partial delivery
                                        message.setChatMessageStatus(ChatMessageStatus.PARTIALLY_RECEIVED);
                                }

                                return message;
                        }).toList();

                        // Batch save all updated messages
                        chatMessageRepo.saveAll(updatedMessages);

                        log.debug("Updated aggregate status for {} messages in conversation {}",
                                        updatedMessages.size(), conversationId);
                } catch (Exception e) {
                        log.error("Failed to update aggregate status for conversation {}",
                                        conversationId, e);
                        // Non-blocking: Exception doesn't affect primary operation
                        // Status will be recalculated on next fetch
                }
        }

        /**
         * Phase 4 (Legacy): Synchronous method for aggregate status updates
         * Kept for backward compatibility. Prefer updateAggregateStatusAsync() for new
         * operations.
         *
         * @deprecated Use updateAggregateStatusAsync(Long conversationId) instead
         */
        @Deprecated
        public ResponseEntity<?> updateAggregateStatus(Timestamp messagesAfter, Long conversationId) {
                try {
                        ChatConversation chatConversation = chatConversationRepo.findById(conversationId).orElseThrow(
                                        () -> new ResourceNotFoundException("ChatConversation", "conversationId",
                                                        conversationId));
                        List<ChatMessage> chatMessages = chatMessageRepo.findByChatConversationIdAndAfter(
                                        conversationId,
                                        messagesAfter);
                        List<ChatMessage> chatMessageList = chatMessages.stream().map(message -> {
                                List<ChatMessageIndividualStatus> chatMessageIndividualStatuses = message
                                                .getChatMessageIndividualStatuses();
                                // received
                                long receivedCount = chatMessageIndividualStatuses.stream()
                                                .filter(status -> status.getStatus().equals(ChatMessageStatus.RECEIVED))
                                                .count();
                                long deliveredCount = chatMessageIndividualStatuses.stream()
                                                .filter(status -> status.getStatus()
                                                                .equals(ChatMessageStatus.DELIVERED))
                                                .count();
                                if (deliveredCount == chatConversation.getTotalParticipants()) {
                                        message.setChatMessageStatus(ChatMessageStatus.DELIVERED);
                                }
                                if (receivedCount == chatConversation.getTotalParticipants()) {
                                        message.setChatMessageStatus(ChatMessageStatus.RECEIVED);
                                }
                                if (receivedCount > 0) {
                                        message.setChatMessageStatus(ChatMessageStatus.PARTIALLY_RECEIVED);
                                }

                                return message;
                        }).toList();

                        chatMessageRepo.saveAll(chatMessageList);
                        return ResponseEntity.ok().build();
                } catch (Exception e) {
                        throw new ServiceLevelException(
                                        "NewChatServiceImpl",
                                        "Failed to update aggregate message status",
                                        "updateAggregateStatus",
                                        e.getClass().getSimpleName(),
                                        e.getMessage());
                }
        }

        /**
         * Validate conversation creation request
         *
         * @throws ServiceLevelException if validation fails
         */
        private void validateConversationCreationRequest(ConversationRequestDto request) {
                if (ObjectUtils.isEmpty(request)) {
                        throw new ServiceLevelException(
                                        "NewChatServiceImpl",
                                        "Conversation request cannot be null",
                                        "validateConversationCreationRequest",
                                        "ValidationException",
                                        "Request body is required");
                }

                if (ObjectUtils.isEmpty(request.getChatConversationType())) {
                        throw new ServiceLevelException(
                                        "NewChatServiceImpl",
                                        "Conversation type is required",
                                        "validateConversationCreationRequest",
                                        "ValidationException",
                                        "Specify DIRECT or GROUP conversation type");
                }

                if (ObjectUtils.isEmpty(request.getChatConversationParticipants())
                                || request.getChatConversationParticipants().isEmpty()) {
                        throw new ServiceLevelException(
                                        "NewChatServiceImpl",
                                        "At least one participant is required",
                                        "validateConversationCreationRequest",
                                        "ValidationException",
                                        "Conversation must have at least one participant");
                }

                // Validate DIRECT conversations have exactly 2 participants
                if (ChatConversationType.DIRECT.equals(request.getChatConversationType())) {
                        if (request.getChatConversationParticipants().size() != 2) {
                                throw new ServiceLevelException(
                                                "NewChatServiceImpl",
                                                "DIRECT conversations must have exactly 2 participants",
                                                "validateConversationCreationRequest",
                                                "ValidationException",
                                                "Direct message conversations must contain exactly 2 participants");
                        }

                        // Check for duplicate DIRECT conversation between same two users
                        List<Long> participantIds = request.getChatConversationParticipants().stream()
                                        .map(p -> p.getParticipantId())
                                        .toList();

                        // TODO: Implement check for existing direct conversation between these
                        // participants
                        // This requires a query: Find conversation where type=DIRECT and has both
                        // participant IDs
                        // For now, logging as enhancement opportunity
                        log.info("Direct conversation creation between participants: {}", participantIds);
                }

                // Validate at least one creator is specified
                long creatorCount = request.getChatConversationParticipants().stream()
                                .filter(p -> Boolean.TRUE.equals(p.getIsChatCreator()))
                                .count();

                if (creatorCount != 1) {
                        throw new ServiceLevelException(
                                        "NewChatServiceImpl",
                                        "Exactly one participant must be marked as creator",
                                        "validateConversationCreationRequest",
                                        "ValidationException",
                                        "Specify exactly one participant with isChatCreator=true");
                }

                // Validate orgId if provided (optional for inter-org conversations)
                if (request.getOrgId() != null && request.getOrgId() <= 0) {
                        throw new ServiceLevelException(
                                        "NewChatServiceImpl",
                                        "Valid organization ID is required",
                                        "validateConversationCreationRequest",
                                        "ValidationException",
                                        "Provide a valid orgId for the conversation");
                }

                // Determine if this is an inter-org conversation
                boolean isInterOrg = false;
                if (request.getIsInterOrg() != null) {
                    isInterOrg = request.getIsInterOrg();
                } else {
                    // Auto-detect: if participants have different orgIds, it's inter-org
                    java.util.Set<Long> orgIds = new java.util.HashSet<>();
                    for (var p : request.getChatConversationParticipants()) {
                        if (p.getOrgId() != null) {
                            orgIds.add(p.getOrgId());
                        }
                    }
                    isInterOrg = orgIds.size() > 1;
                }
                request.setIsInterOrg(isInterOrg);
        }

        @Override
        @Transactional
        public ResponseEntity<?> addParticipants(Long conversationId, java.util.List<ChatConversationParticipantDto> participants) {
                try {
                        ChatConversation chatConversation = chatConversationRepo.findById(conversationId).orElseThrow(
                                        () -> new ResourceNotFoundException("ChatConversation", "conversationId", conversationId));
                        if (participants == null) {
                                return ResponseEntity.badRequest().body("Participants are required");
                        }
                        java.util.Set<Long> existingIds = chatConversation.getChatConversationParticipants().stream()
                                        .map(ChatConversationParticipant::getParticipantId).collect(java.util.stream.Collectors.toSet());
                        for (ChatConversationParticipantDto dto : participants) {
                                if (dto.getParticipantId() == null || existingIds.contains(dto.getParticipantId())) {
                                        continue;
                                }
                                ChatConversationParticipant p = new ChatConversationParticipant();
                                p.setParticipantId(dto.getParticipantId());
                                p.setParticipantName(dto.getParticipantName());
                                p.setParticipantEmail(dto.getParticipantEmail());
                                p.setParticipantMob(dto.getParticipantMob());
                                p.setParticipantRole(dto.getParticipantRole());
                                p.setParticipantAvatar(dto.getParticipantAvatar());
                                p.setIsChatCreator(Boolean.TRUE.equals(dto.getIsChatCreator()));
                                p.setChatParticipantType(dto.getChatParticipantType());
                                p.setChatParticipantStatus(ChatParticipantStatus.JOINED);
                                p.setChatConversation(chatConversation);
                                if (dto.getOrgId() != null) p.setOrgId(dto.getOrgId());
                                if (dto.getOrgName() != null) p.setOrgName(dto.getOrgName());
                                if (dto.getOrgType() != null) p.setOrgType(dto.getOrgType());
                                chatConversation.getChatConversationParticipants().add(p);
                                existingIds.add(dto.getParticipantId());
                        }
                        chatConversation.setTotalParticipants((long) chatConversation.getChatConversationParticipants().size());
                        chatConversationRepo.save(chatConversation);
                        return ResponseEntity.ok(chatConversation);
                } catch (Exception e) {
                        throw new ServiceLevelException("NewChatServiceImpl", "Failed to add participants", "addParticipants",
                                        e.getClass().getSimpleName(), e.getMessage());
                }
        }

        @Override
        @Transactional
        public ResponseEntity<?> removeParticipant(Long conversationId, Long participantId) {
                try {
                        ChatConversation chatConversation = chatConversationRepo.findById(conversationId).orElseThrow(
                                        () -> new ResourceNotFoundException("ChatConversation", "conversationId", conversationId));
                        ChatConversationParticipant target = chatConversation.getChatConversationParticipants().stream()
                                        .filter(p -> participantId.equals(p.getParticipantId())).findFirst().orElse(null);
                        if (target == null) {
                                return ResponseEntity.notFound().build();
                        }
                        target.setChatParticipantStatus(ChatParticipantStatus.LEFT);
                        target.setIsActive(false);
                        chatConversation.getChatConversationParticipants().remove(target);
                        chatConversation.setTotalParticipants((long) chatConversation.getChatConversationParticipants().size());
                        chatConversationRepo.save(chatConversation);
                        return ResponseEntity.ok().build();
                } catch (Exception e) {
                        throw new ServiceLevelException("NewChatServiceImpl", "Failed to remove participant", "removeParticipant",
                                        e.getClass().getSimpleName(), e.getMessage());
                }
        }
}
