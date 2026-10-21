package com.nexus.cms.notification.repositories;

import com.nexus.cms.notification.entities.NotificationFailure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationFailureRepo extends JpaRepository<NotificationFailure, Long> {

    Optional<NotificationFailure> findByEventId(String eventId);

    /**
     * Failures that are still waiting to be replayed, oldest first.
     * <p>
     * The status is a code from
     * {@code cms.t_notification_failure_status_master}, not a Java enum.
     */
    List<NotificationFailure> findByStatusOrderByCreatedAtAsc(String status);

    /**
     * How many failures are waiting to be replayed.
     */
    long countByStatus(String status);

    /**
     * Failures recorded before the given instant, used by the retention job.
     */
    @Query("SELECT f FROM NotificationFailure f WHERE f.createdAt < :cutoff")
    List<NotificationFailure> findCreatedBefore(@Param("cutoff") Instant cutoff);

    /**
     * The failures for a given event type, for dashboards.
     */
    @Query("SELECT f FROM NotificationFailure f WHERE f.eventType = :eventType ORDER BY f.createdAt DESC")
    List<NotificationFailure> findByEventType(@Param("eventType") String eventType);
}
