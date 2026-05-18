package com.nexus.cms.service;

import com.nexus.cms.exception.ResourceNotFoundException;
import com.nexus.cms.exception.ServiceLevelException;
import com.nexus.cms.mapper.ConversationMapper;
import com.nexus.cms.model.entities.Conversation;
import com.nexus.cms.model.entities.ConversationParticipant;
import com.nexus.cms.payload.ChatPayload;
import com.nexus.cms.model.entities.Conversation.ConversationType;
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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final ConversationMapper conversationMapper;

    /**
     * Create a new conversation (DIRECT 1-1 or GROUP)
     * Single transaction approach - cascade relationships removed so no isolation
     * issues
     */
    @Transactional
    public Conversation createConversation(
            Conversation.ConversationType type,
            String name,
            Long creatorUserId,
            List<Long> participantIds,
            Long orgId) {

        // Validation
        validateCreateConversationRequest(type, creatorUserId, participantIds, orgId);

        // For DIRECT conversations, check if one already exists
        if (type == Conversation.ConversationType.DIRECT) {
            Optional<Conversation> existingConv = findExistingDirectConversation(orgId, participantIds);
            if (existingConv.isPresent()) {
                log.info("Direct conversation already exists between users {} and {}",
                        participantIds.get(0), participantIds.get(1));
                return existingConv.get();
            }
        }

        try {
            // Step 1: Create conversation entity
            Conversation conversation = buildConversation(type, name, creatorUserId, orgId, participantIds.size());

            // Use repository save for new entities - handles all entity lifecycle properly
            Conversation savedConversation = conversationRepository.save(conversation);
            log.info("Created {} conversation with ID: {}", type, savedConversation.getId());

            // Step 2: Create and save participants directly (no cascade issues since we
            // removed @OneToMany)
            Set<Long> uniqueParticipants = new HashSet<>(participantIds);
            List<ConversationParticipant> participants = new ArrayList<>();

            for (Long userId : uniqueParticipants) {
                ConversationParticipant participant = ConversationParticipant.builder()
                        .conversation(savedConversation)
                        .userId(userId)
                        .joinedAt(new Timestamp(System.currentTimeMillis()))
                        .build();
                if (creatorUserId.equals(userId)) {
                    participant.setIsPrimary(true);
                }
                participants.add(participant);
            }

            participantRepository.saveAll(participants);
            log.info("Added {} participants to conversation {}", uniqueParticipants.size(), savedConversation.getId());

            // Return the conversation
            return savedConversation;

        } catch (ServiceLevelException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating conversation", e);
            throw new ServiceLevelException(
                    "ConversationService",
                    "Error occurred while creating conversation",
                    "createConversation",
                    "Service level exception",
                    e.getMessage());
        }
    }

    /**
     * Get conversation by ID with organization check
     */
    @Transactional(readOnly = true)
    public Conversation getConversation(Long conversationId, Long orgId) {
        if (ObjectUtils.isEmpty(conversationId) || ObjectUtils.isEmpty(orgId)) {
            throw new ServiceLevelException(
                    "ConversationService",
                    "Conversation ID and Organization ID are required",
                    "getConversation",
                    "Missing required data",
                    "conversationId and orgId cannot be null");
        }

        return conversationRepository.findByIdAndOrgId(conversationId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conversation",
                        "id",
                        conversationId.toString()));
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
                    "orgId cannot be null");
        }

        return conversationRepository.findByOrgIdAndIsActive(orgId, isActive != null ? isActive : true, pageable);
    }

    /**
     * Get all conversations where user is a participant
     */
    @Transactional(readOnly = true)
    public Page<ChatPayload.ConversationSummary> getUserConversations(Long userId, Long orgId, Pageable pageable) {
        if (ObjectUtils.isEmpty(userId) || ObjectUtils.isEmpty(orgId)) {
            throw new ServiceLevelException(
                    "ConversationService",
                    "User ID and Organization ID are required",
                    "getUserConversations",
                    "Missing required data",
                    "userId and orgId cannot be null");
        }

        Page<Conversation> userConversations = conversationRepository.findUserConversations(orgId, userId, pageable);
        Page<ChatPayload.ConversationSummary> summaries = userConversations
                .map(conversation -> conversationMapper.toConversationSummary(conversation, userId));

        return summaries;

    }

    /**
     * Add participant to conversation
     */
    @Transactional
    public void addParticipant(Long conversationId, Long userId, Long orgId) {
        validateParticipantOperation(conversationId, userId, orgId);

        try {
            // Verify conversation exists
            Conversation conversation = getConversation(conversationId, orgId);

            // Check if already participant
            if (participantRepository.existsByConversationIdAndUserId(conversationId, userId)) {
                log.warn("User {} already participant of conversation {}", userId, conversationId);
                return;
            }

            ConversationParticipant participant = ConversationParticipant.builder()
                    .conversation(conversation)
                    .userId(userId)
                    .joinedAt(new Timestamp(System.currentTimeMillis()))
                    .build();

            participantRepository.save(participant);

            // Update participant count
            updateParticipantCount(conversationId);

            log.info("Added user {} to conversation {}", userId, conversationId);

        } catch (ServiceLevelException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error adding participant to conversation", e);
            throw new ServiceLevelException(
                    "ConversationService",
                    "Error occurred while adding participant",
                    "addParticipant",
                    "Service level exception",
                    e.getMessage());
        }
    }

    /**
     * Remove participant from conversation
     */
    @Transactional
    public void removeParticipant(Long conversationId, Long userId, Long orgId) {
        validateParticipantOperation(conversationId, userId, orgId);

        try {
            // Verify conversation exists and belongs to org
            getConversation(conversationId, orgId);

            // Verify participant exists
            ConversationParticipant participant = participantRepository
                    .findByConversationIdAndUserId(conversationId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "ConversationParticipant",
                            "conversationId and userId",
                            conversationId + ":" + userId));

            participantRepository.delete(participant);

            // Update participant count
            updateParticipantCount(conversationId);

            log.info("Removed user {} from conversation {}", userId, conversationId);

        } catch (ServiceLevelException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error removing participant from conversation", e);
            throw new ServiceLevelException(
                    "ConversationService",
                    "Error occurred while removing participant",
                    "removeParticipant",
                    "Service level exception",
                    e.getMessage());
        }
    }

    /**
     * Validate user is participant of conversation
     */
    @Transactional(readOnly = true)
    public boolean isUserParticipant(Long conversationId, Long userId) {
        return participantRepository.existsByConversationIdAndUserId(conversationId, userId);
    }

    /**
     * Get all participants in conversation
     */
    @Transactional(readOnly = true)
    public List<ConversationParticipant> getParticipants(Long conversationId) {
        return participantRepository.findByConversationId(conversationId);
    }

    /**
     * Get participant count for conversation
     */
    @Transactional(readOnly = true)
    public int getParticipantCount(Long conversationId) {
        return (int) participantRepository.countByConversationId(conversationId);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Validate create conversation request
     */
    private void validateCreateConversationRequest(
            Conversation.ConversationType type,
            Long creatorUserId,
            List<Long> participantIds,
            Long orgId) {

        if (ObjectUtils.isEmpty(creatorUserId) || ObjectUtils.isEmpty(orgId)) {
            throw new ServiceLevelException(
                    "ConversationService",
                    "Creator ID and Organization ID are required",
                    "createConversation",
                    "Missing required data",
                    "creatorUserId and orgId cannot be null");
        }

        if (type == Conversation.ConversationType.DIRECT) {
            if (participantIds == null || participantIds.size() != 2) {
                throw new ServiceLevelException(
                        "ConversationService",
                        "DIRECT conversations must have exactly 2 participants",
                        "createConversation",
                        "Invalid participant count",
                        "Expected 2 participants, got " + (participantIds == null ? 0 : participantIds.size()));
            }
        }

        if (type == Conversation.ConversationType.GROUP) {
            if (participantIds == null || participantIds.size() < 2) {
                throw new ServiceLevelException(
                        "ConversationService",
                        "GROUP conversations must have at least 2 participants",
                        "createConversation",
                        "Invalid participant count",
                        "Expected at least 2 participants, got "
                                + (participantIds == null ? 0 : participantIds.size()));
            }
        }
    }

    /**
     * Find existing direct conversation between two users
     */
    private Optional<Conversation> findExistingDirectConversation(Long orgId, List<Long> participantIds) {
        Long userId1 = participantIds.get(0);
        Long userId2 = participantIds.get(1);
        return conversationRepository.findDirectConversation(orgId, userId1, userId2);
    }

    /**
     * Build conversation entity
     */
    private Conversation buildConversation(
            Conversation.ConversationType type,
            String name,
            Long creatorUserId,
            Long orgId,
            int participantCount) {

        return Conversation.builder()
                .name(name)
                .type(type)
                .createdBy(creatorUserId)
                .orgId(orgId)
                .isActive(true)
                .participantCount(participantCount)
                .build();
    }

    /**
     * Validate participant operation
     */
    private void validateParticipantOperation(Long conversationId, Long userId, Long orgId) {
        if (ObjectUtils.isEmpty(conversationId) || ObjectUtils.isEmpty(userId) || ObjectUtils.isEmpty(orgId)) {
            throw new ServiceLevelException(
                    "ConversationService",
                    "Conversation ID, User ID, and Organization ID are required",
                    "participantOperation",
                    "Missing required data",
                    "conversationId, userId, and orgId cannot be null");
        }
    }

    /**
     * Update participant count denormalization
     */
    @Transactional
    protected void updateParticipantCount(Long conversationId) {
        int count = getParticipantCount(conversationId);
        conversationRepository.updateParticipantCount(conversationId, count);
    }
}
