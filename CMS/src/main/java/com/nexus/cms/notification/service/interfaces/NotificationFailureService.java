package com.nexus.cms.notification.service.interfaces;

import com.nexus.cms.notification.model.DomainEventDto;

/**
 * Records an event that could not be projected, and replays it.
 */
public interface NotificationFailureService {

    /**
     * Record a failed event so that it can be inspected and replayed.
     *
     * @param event        the event that failed, may be null when the payload
     *                     could not be parsed
     * @param rawEvent     the event exactly as it was received
     * @param errorType    the type of the failure
     * @param errorMessage the message of the failure
     * @param sourceTopic       the topic the event was consumed from
     * @param sourcePartition   the partition it was consumed from
     * @param sourceOffset      the offset it was consumed from
     */
    void recordFailure(DomainEventDto event,
                       String rawEvent,
                       String errorType,
                       String errorMessage,
                       String sourceTopic,
                       Integer sourcePartition,
                       Long sourceOffset);

    /**
     * Replay the failures that are waiting to be retried.
     *
     * @return how many events were republished
     */
    int replayPendingFailures();
}
