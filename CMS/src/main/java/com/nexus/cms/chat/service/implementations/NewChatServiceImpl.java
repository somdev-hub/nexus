package com.nexus.cms.chat.service.implementations;

import com.nexus.cms.chat.entities.*;
import com.nexus.cms.chat.enums.ChatConversationType;
import com.nexus.cms.chat.enums.ChatMessageStatus;
import com.nexus.cms.chat.enums.ChatMessageType;
import com.nexus.cms.chat.enums.ChatParticipantStatus;
import com.nexus.cms.chat.model.ChatConversationDetailedSummary;
import com.nexus.cms.chat.model.ChatConversationQuickResponseDto;
import com.nexus.cms.chat.model.ConversationMessagesDto;
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
        private final ChatConversationParticipantRepo chatConversationParticipantRepo;
        private final ChatMessageIndividualStatusRepo chatMessageIndividualStatusRepo;

        @Transactional(timeout = 30)
        @Override
        public ResponseEntity<?> createChatConversation(ConversationRequestDto request,
                        MultipartFile chatConversationAvatar) {
                try {
                        // Validate request before processing
                        validateConversationCreationRequest(request);

                        List<ChatConversationParticipant> chatConversationParticipants = new ArrayList<>(
                                        request.getChatConversationParticipants().stream()
                                                        .map(chatConversationParticipantDto -> {
                                                                ChatConversationParticipant conversationParticipant = modelMapper
                                                                                .map(chatConversationParticipantDto,
                                                                                                ChatConversationParticipant.class);
                                                                conversationParticipant.setChatParticipantStatus(
                                                                                ChatParticipantStatus.JOINED);
                                                                return conversationParticipant;
                                                        }).toList());

                        ChatConversation chatConversation = modelMapper.map(request, ChatConversation.class);
                        chatConversationParticipants
                                        .forEach(participant -> participant.setChatConversation(chatConversation));
                        chatConversation.setChatConversationParticipants(chatConversationParticipants);
                        chatConversation.setTotalParticipants((long) chatConversationParticipants.size());
                        chatConversation.setTotalMessages(0L);

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

                                ResponseEntity<?> dmsResponse = restService.cmsRestCall(url.toUriString(), payload,
                                                headers,
                                                HttpMethod.POST, request.getOrgId());
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
                        throw new ServiceLevelException(
                                        "NewChatServiceImpl",
                                        "Failed to create chat conversation",
                                        "createChatConversation",
                                        e.getClass().getSimpleName(),
                                        e.getMessage());
                }
        }

        @Override
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
                Timestamp lastRead = chatConversationParticipant.getLastRead();

                // Fix N+1: Use direct database query instead of loading all messages
                long count = chatMessageRepo.countUnreadMessages(
                                conversation.getChatConversationId(),
                                lastRead);

                // get latest message
                ChatMessage latestMessage = conversation.getChatMessages().isEmpty()
                                ? null
                                : conversation.getChatMessages().getLast();

                if (!ObjectUtils.isEmpty(latestMessage) && latestMessage!=null) {
                        if (latestMessage.getChatMessageType()
                                        .equals(ChatMessageType.TEXT)) {
                                chatConversationQuickResponseDto.setLastMessage(
                                                latestMessage.getChatMessageText());
                        } else {
                                chatConversationQuickResponseDto
                                                .setLastMessage(latestMessage
                                                                .getChatMessageType()
                                                                .name());
                        }
                        chatConversationQuickResponseDto
                                        .setLastMessageAt(latestMessage.getSentAt());
                        chatConversationQuickResponseDto.setLastMessageSenderId(
                                        latestMessage.getChatConversationParticipant()
                                                        .getParticipantId());
                        chatConversationQuickResponseDto.setLastMessageSenderName(
                                        latestMessage.getChatConversationParticipant()
                                                        .getParticipantName());
                }
                chatConversationQuickResponseDto.setUnreadCount(count);

                if (conversation.getChatConversationType()
                                .equals(ChatConversationType.DIRECT)) {
                        ChatConversationParticipant otherParticipant = conversation
                                        .getChatConversationParticipants().stream()
                                        .filter(participant -> !participant.equals(
                                                        chatConversationParticipant))
                                        .findFirst()
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Other participant not found"));
                        chatConversationQuickResponseDto
                                        .setOtherParticipantName(otherParticipant
                                                        .getParticipantName());
                        chatConversationQuickResponseDto
                                        .setOtherParticipantAvatar(otherParticipant
                                                        .getParticipantAvatar());
                        chatConversationQuickResponseDto.setOtherParticipantId(
                                        otherParticipant.getParticipantId());
                }

                return chatConversationQuickResponseDto;
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
                        updateAggregateStatusAsync(conversationId);

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
                                List<ConversationMessagesDto.MessageSeenBy> messageSeenByList = message
                                                .getChatConversation()
                                                .getChatConversationParticipants()
                                                .stream()
                                                .filter(participant -> participant.getLastRead() != null
                                                                && participant.getLastRead().after(message.getSentAt()))
                                                .map(participant -> modelMapper.map(participant,
                                                                ConversationMessagesDto.MessageSeenBy.class))
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
                                        e.getMessage());
                }
        }

        @Override
        public ResponseEntity<?> getChatConversation(Long conversationId, Long participantId) {
                try {
                        ChatConversation chatConversation = chatConversationRepo.findById(conversationId).orElseThrow(
                                        () -> new ResourceNotFoundException("ChatConversation", "conversationId",
                                                        conversationId));
                        ChatConversationDetailedSummary chatConversationDetailedSummary = modelMapper.map(
                                        chatConversation,
                                        ChatConversationDetailedSummary.class);
                        chatConversation.getChatMessages().forEach(message -> {
                                List<ChatMessageAttachment> chatMessageAttachmentList = message
                                                .getChatMessageAttachmentList();
                                List<ChatMessageAttachment> imageAndVideoAttachments = chatMessageAttachmentList
                                                .stream()
                                                .filter(attachment -> attachment.getAttachmentType() != null &&
                                                                CommonConstants.IMAGE_AND_VIDEO_ATTACHMENT_TYPES
                                                                                .contains(attachment
                                                                                                .getAttachmentType()))
                                                .toList();
                                List<ChatMessageAttachment> fileAttachments = chatMessageAttachmentList.stream()
                                                .filter(attachment -> attachment.getAttachmentType() != null &&
                                                                CommonConstants.FILE_ATTACHMENT_TYPES.contains(
                                                                                attachment.getAttachmentType()))
                                                .toList();
                                chatConversationDetailedSummary.getImageAndVideoAttachments()
                                                .addAll(imageAndVideoAttachments);
                                chatConversationDetailedSummary.getFileAttachments().addAll(fileAttachments);
                        });
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
                        modelMapper.map(request, chatConversation);
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

                                ResponseEntity<?> dmsResponse = restService.cmsRestCall(url.toUriString(), payload,
                                                headers,
                                                HttpMethod.POST, request.getOrgId());
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
        public void updateAggregateStatusAsync(Long conversationId) {
                try {
                        ChatConversation chatConversation = chatConversationRepo.findById(conversationId)
                                        .orElseThrow(
                                                        () -> new ResourceNotFoundException("ChatConversation",
                                                                        "conversationId", conversationId));

                        // Get all messages in conversation that need status recalculation
                        List<ChatMessage> chatMessages = chatConversation.getChatMessages();

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

                // Validate orgId if provided
                if (request.getOrgId() == null || request.getOrgId() <= 0) {
                        throw new ServiceLevelException(
                                        "NewChatServiceImpl",
                                        "Valid organization ID is required",
                                        "validateConversationCreationRequest",
                                        "ValidationException",
                                        "Provide a valid orgId for the conversation");
                }
        }
}
