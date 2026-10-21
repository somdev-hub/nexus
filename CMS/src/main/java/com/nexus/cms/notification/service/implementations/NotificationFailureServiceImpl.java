package com.nexus.cms.notification.service.implementations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.cms.notification.entities.NotificationFailure;
import com.nexus.cms.notification.properties.NotificationMasterCodes;
import com.nexus.cms.notification.model.DomainEventDto;
import com.nexus.cms.notification.properties.NotificationConstants;
import com.nexus.cms.notification.properties.NotificationProperties;
import com.nexus.cms.notification.repositories.NotificationFailureRepo;
import com.nexus.cms.notification.service.interfaces.NotificationFailureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationFailureServiceImpl implements NotificationFailureService {

    private final NotificationFailureRepo failureRepo;
    private final NotificationProperties properties;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional
    public void recordFailure(DomainEventDto event,
                              String rawEvent,
                              String errorType,
                              String errorMessage,
                              String sourceTopic,
                              Integer sourcePartition,
                              Long sourceOffset) {
        if (!properties.isDlqTrackerEnabled()) {
            log.debug("Failure tracking is disabled — the event is not recorded");
            return;
        }
        try {
            String eventId = event != null ? event.getEventId() : null;
            if (eventId == null) {
                // An event that could not be parsed has no id. Derive a
                // stable one from the payload so that repeated deliveries do
                // not create a row each time.
                eventId = eventIdForUnparsable(rawEvent);
            }
            if (eventId != null && failureRepo.findByEventId(eventId).isPresent()) {
                log.debug("Failure for event {} is already recorded", eventId);
                return;
            }

            NotificationFailure failure = new NotificationFailure();
            failure.setEventId(eventId);
            failure.setEventType(event != null ? event.getEventType() : null);
            failure.setSourceTopic(sourceTopic != null ? sourceTopic : NotificationConstants.EVENTS_TOPIC);
            failure.setSourcePartition(sourcePartition);
            failure.setSourceOffset(sourceOffset);
            failure.setPayload(rawEvent);
            failure.setErrorType(errorType);
            failure.setErrorMessage(errorMessage);
            failure.setAttempts(0);
            failure.setReplayCount(0);
            failure.setStatus(NotificationMasterCodes.FAILURE_STATUS_FAILED);
            failure.setCreatedAt(Timestamp.from(Instant.now()));

            failureRepo.save(failure);
            log.error("Recorded failure for event {} — it is waiting to be replayed", eventId);
        } catch (Exception e) {
            // Recording the failure must never mask the original failure.
            log.error("Could not record the failure — the event is only preserved in the dead letter topic", e);
        }
    }

    @Override
    @Transactional
    public int replayPendingFailures() {
        if (!properties.isDlqReplayEnabled()) {
            return 0;
        }
        List<NotificationFailure> pending = failureRepo.findByStatusOrderByCreatedAtAsc(
                NotificationMasterCodes.FAILURE_STATUS_FAILED);
        if (pending.isEmpty()) {
            return 0;
        }

        Instant cooldownBefore = Instant.now().minusSeconds(properties.getDlqReplayCooldownSeconds());
        int replayed = 0;

        for (NotificationFailure failure : pending) {
            if (failure.getCreatedAt() == null
                    || failure.getCreatedAt().toInstant().isAfter(cooldownBefore)) {
                // Not old enough to be retried yet.
                continue;
            }
            if (failure.getReplayCount() != null
                    && failure.getReplayCount() >= properties.getDlqReplayMaxReplays()) {
                failure.setStatus(NotificationMasterCodes.FAILURE_STATUS_PERMANENT);
                failureRepo.save(failure);
                log.error("Event {} has been replayed {} times and has been given up on",
                        failure.getEventId(), failure.getReplayCount());
                continue;
            }
            try {
                kafkaTemplate.send(NotificationConstants.EVENTS_TOPIC,
                        failure.getEventId(),
                        failure.getPayload());

                failure.setReplayCount(failure.getReplayCount() + 1);
                failure.setReplayedAt(Timestamp.from(Instant.now()));
                failure.setStatus(NotificationMasterCodes.FAILURE_STATUS_REPLAYED);
                failureRepo.save(failure);
                replayed++;
                log.info("Replayed failed event {} (attempt {})", failure.getEventId(), failure.getReplayCount());
            } catch (Exception e) {
                log.error("Could not replay event {}", failure.getEventId(), e);
            }
        }
        if (replayed > 0) {
            log.info("Replayed {} failed events", replayed);
        }
        return replayed;
    }

    /**
     * Derive a stable identifier for an event that could not be parsed, so
     * that repeated deliveries of the same poison message do not each create
     * a failure row.
     */
    private String eventIdForUnparsable(String rawEvent) {
        if (rawEvent == null || rawEvent.isBlank()) {
            return null;
        }
        try {
            JsonNode parsed = objectMapper.readTree(rawEvent);
            JsonNode node = parsed.get("eventId");
            if (node != null && !node.isNull()) {
                return node.asText();
            }
        } catch (Exception ignored) {
            // Fall through to the hash.
        }
        return "unparsable-" + Integer.toHexString(rawEvent.hashCode());
    }
}
