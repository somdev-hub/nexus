package com.nexus.cms.service;

import com.nexus.cms.exception.ResourceNotFoundException;
import com.nexus.cms.exception.ServiceLevelException;
import com.nexus.cms.model.entities.Conversation;
import com.nexus.cms.model.entities.ConversationParticipant;
import com.nexus.cms.repository.ConversationParticipantRepository;
import com.nexus.cms.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;

    /**
     * Create a new conversation (DIRECT 1-1 or GROUP)
     *
     * @param type              DIRECT or GROUP
     * @param name              Conversation name (optional for DIRECT, required for GROUP)
     * @param creatorUserId     User ID of conversation creator
     * @param participantIds    List of participant user IDs (including creator)
     * @param orgId             Organization ID for multi-tenancy
     * @return                  Created Conversation entity
     */
    @Transactional
    public Conversation createConversation(
            Conversation.ConversationType type,
            String name,
            Long creatorUserId,
            List<Long> participantIds,
            Long orgId) {

        if (ObjectUtils.isEmpty(creatorUserId) || ObjectUtils.isEmpty(orgId)) {
            throw new ServiceLevelException(
                    "ConversationService",
                    "Creator ID and Organization ID are required",
                    "createConversation",
                    "Missing required data",
                    "creatorUserId and orgId cannot be null"
            );
        }

        // Validate participant count based on type
        if (type == Conversation.ConversationType.DIRECT && (participantIds == null || participantIds.size() != 2)) {
            throw new ServiceLevelException(
                    "ConversationService",
                    "DIRECT conversations must have exactly 2 participants",
                    "createConversation",
                    "Invalid participant count",
                    "Expected 2 participants, got " + (participantIds == null ? 0 : participantIds.size())
            );
        }

        if (type == Conversation.ConversationType.GROUP && (participantIds == null || participantIds.size() < 2)) {
            throw new ServiceLevelException(
                    "ConversationService",
                    "GROUP conversations must have at least 2 participants",
                    "createConversation",
                    "Invalid participant count",
                    "Expected at least 2 participants, got " + (participantIds == null ? 0 : participantIds.size())
            );
        }

        // For DIRECT conversations, check if one already exists
        if (type == Conversation.ConversationType.DIRECT && participantIds.size() == 2) {
            Long userId1 = participantIds.get(0);
            Long userId2 = participantIds.get(1);
            var existingConv = conversationRepository.findDirectConversation(orgId, userId1, userId2);
            if (existingConv.isPresent()) {
                log.info("Direct conversation already exists between users {} and {}", userId1, userId2);
                return existingConv.get();
            }
        }

        try {
            // Create conversation
            Conversation conversation = Conversation.builder()
                    .id(UUID.randomUUID())
                    .name(name)
                    .type(type)
                    .createdBy(creatorUserId)
                    .orgId(orgId)
                    .isActive(true)
                    .build();

            Conversation savedConversation = conversationRepository.save(conversation);
            log.info("Created {} conversation with ID: {}", type, savedConversation.getId());

            // Add participants
            Set<Long> uniqueParticipants = new HashSet<>(participantIds);
            for (Long userId : uniqueParticipants) {
                ConversationParticipant participant = ConversationParticipant.builder()
                        .id(UUID.randomUUID())
                        .conversation(savedConversation)
                        .userId(userId)
                        .joinedAt(new Timestamp(System.currentTimeMillis()))
                        .build();
                participantRepository.save(participant);
            }

            log.info("Added {} participants to conversation {}", uniqueParticipants.size(), savedConversation.getId());
            return savedConversation;

        } catch (Exception e) {
            log.error("Error creating conversation", e);
            throw new ServiceLevelException(
                    "ConversationService",
                    "Error occurred while creating conversation",
                    "createConversation",
                    "Service level exception",
                    e.getMessage()
            );
        }
    }

    /**
     * Get conversation by ID with organization check
     */
    @Transactional(readOnly = true)
    public Conversation getConversation(UUID conversationId, Long orgId) {
        if (ObjectUtils.isEmpty(conversationId) || ObjectUtils.isEmpty(orgId)) {
            throw new ServiceLevelException(
                    "ConversationService",
                    "Conversation ID and Organization ID are required",
                    "getConversation",
                    "Missing required data",
                    "conversationId and orgId cannot be null"
            );
        }

        return conversationRepository.findByIdAndOrgId(conversationId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conversation",
                        "id",
                        conversationId.toString()
                ));
    }

    /**
     * Get all conversations for an organization
     */
    @Transactional(readOnly = true)
    public Page<Conversation> getOrganizationConversations(Long orgId, Boolean isActive, Pageable pageable) {
        if (ObjectUtils.isEmpty(orgId)) {
            throw new ServiceLevelException(
                    "ConversationService",
                    "Organization ID is required",
                    "getOrganizationConversations",
                    "Missing required data",
                    "orgId cannot be null"
            );
        }

        return conversationRepository.findByOrgIdAndIsActive(orgId, isActive != null ? isActive : true, pageable);
    }

    /**
     * Get all conversations where user is a participant
     */
    @Transactional(readOnly = true)
    public Page<Conversation> getUserConversations(Long userId, Long orgId, Pageable pageable) {
        if (ObjectUtils.isEmpty(userId) || ObjectUtils.isEmpty(orgId)) {
            throw new ServiceLevelException(
                    "ConversationService",
                    "User ID and Organization ID are required",
                    "getUserConversations",
                    "Missing required data",
                    "userId and orgId cannot be null"
            );
        }

        return conversationRepository.findUserConversations(orgId, userId, pageable);
    }

    /**
     * Add participant to conversation
     */
    @Transactional
    public void addParticipant(UUID conversationId, Long userId, Long orgId) {
        if (ObjectUtils.isEmpty(conversationId) || ObjectUtils.isEmpty(userId) || ObjectUtils.isEmpty(orgId)) {
            throw new ServiceLevelException(
                    "ConversationService",
                    "Conversation ID, User ID, and Organization ID are required",
                    "addParticipant",
                    "Missing required data",
                    "conversationId, userId, and orgId cannot be null"
            );
        }

        try {
            // Verify conversation exists
            Conversation conversation = getConversation(conversationId, orgId);

            // Check if already participant
            if (participantRepository.existsByConversationIdAndUserId(conversationId, userId)) {
                log.warn("User {} already participant of conversation {}", userId, conversationId);
                return;
            }

            ConversationParticipant participant = ConversationParticipant.builder()
                    .id(UUID.randomUUID())
                    .conversation(conversation)
                    .userId(userId)
                    .joinedAt(new Timestamp(System.currentTimeMillis()))
                    .build();

            participantRepository.save(participant);
            log.info("Added user {} to conversation {}", userId, conversationId);

        } catch (Exception e) {
            log.error("Error adding participant to conversation", e);
            throw new ServiceLevelException(
                    "ConversationService",
                    "Error occurred while adding participant",
                    "addParticipant",
                    "Service level exception",
                    e.getMessage()
            );
        }
    }

    /**
     * Remove participant from conversation
     */
    @Transactional
    public void removeParticipant(UUID conversationId, Long userId, Long orgId) {
        if (ObjectUtils.isEmpty(conversationId) || ObjectUtils.isEmpty(userId) || ObjectUtils.isEmpty(orgId)) {
            throw new ServiceLevelException(
                    "ConversationService",
                    "Conversation ID, User ID, and Organization ID are required",
                    "removeParticipant",
                    "Missing required data",
                    "conversationId, userId, and orgId cannot be null"
            );
        }

        try {
            // Verify conversation exists and belongs to org
            getConversation(conversationId, orgId);

            // Verify participant exists
            var participant = participantRepository.findByConversationIdAndUserId(conversationId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "ConversationParticipant",
                            "conversationId and userId",
                            conversationId + ":" + userId
                    ));

            participantRepository.delete(participant);
            log.info("Removed user {} from conversation {}", userId, conversationId);

        } catch (Exception e) {
            log.error("Error removing participant from conversation", e);
            throw new ServiceLevelException(
                    "ConversationService",
                    "Error occurred while removing participant",
                    "removeParticipant",
                    "Service level exception",
                    e.getMessage()
            );
        }
    }

    /**
     * Validate user is participant of conversation
     */
    @Transactional(readOnly = true)
    public boolean isUserParticipant(UUID conversationId, Long userId) {
        return participantRepository.existsByConversationIdAndUserId(conversationId, userId);
    }

    /**
     * Get all participants in conversation
     */
    @Transactional(readOnly = true)
    public List<ConversationParticipant> getParticipants(UUID conversationId) {
        return participantRepository.findByConversationId(conversationId);
    }
}

