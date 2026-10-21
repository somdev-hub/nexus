package com.nexus.cms.notification.service.implementations;

import com.nexus.cms.notification.service.interfaces.NotificationFailureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Republishes the events that could not be processed.
 * <p>
 * A transient failure — the database was briefly unavailable, a dependency
 * was restarting — should not need anybody to intervene. The replay job picks
 * the failures that have been waiting long enough and puts them back on the
 * source topic, up to a limit. Past the limit the failure is given up on and
 * kept for manual inspection, so a genuinely poisonous event cannot loop
 * forever.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationReplayJob {

    private final NotificationFailureService failureService;

    /**
     * Runs every minute.
     */
    @Scheduled(fixedDelayString = "${notification.dlq.replay.interval-ms:60000}")
    public void replayFailedEvents() {
        try {
            int replayed = failureService.replayPendingFailures();
            if (replayed > 0) {
                log.info("Replayed {} failed events", replayed);
            }
        } catch (Exception e) {
            log.error("Failed to replay failed events", e);
        }
    }
}
