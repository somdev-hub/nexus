package com.nexus.cms.notification.repositories;

import com.nexus.cms.notification.entities.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepo extends JpaRepository<Notification, Long> {

    /**
     * Look a notification up by the event it was projected from.
     * <p>
     * The event id is unique, so this returns at most one row. The projection
     * uses it to decide whether an event has already been turned into a
     * notification.
     */
    Optional<Notification> findByEventId(String eventId);

    /**
     * The most recent notifications for an organisation.
     */
    @Query("SELECT n FROM Notification n WHERE n.targetOrgId = :orgId ORDER BY n.createdAt DESC, n.notificationId DESC")
    Page<Notification> findRecentForOrg(@Param("orgId") Long orgId, Pageable pageable);

    /**
     * The most recent notifications for an organisation, as a plain list.
     */
    @Query("SELECT n FROM Notification n WHERE n.targetOrgId = :orgId ORDER BY n.createdAt DESC, n.notificationId DESC")
    List<Notification> findRecentListForOrg(@Param("orgId") Long orgId, Pageable pageable);

    /**
     * How many notifications an organisation has, regardless of who has read
     * them.
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.targetOrgId = :orgId")
    long countForOrg(@Param("orgId") Long orgId);

    /**
     * How many notifications the user has not read in the organisation.
     */
    @Query("""
            SELECT COUNT(n) FROM Notification n
            WHERE n.targetOrgId = :orgId
            AND NOT EXISTS (
                SELECT r FROM NotificationReadReceipt r
                WHERE r.notification = n AND r.userId = :userId)
            """)
    long countUnreadForUserInOrg(@Param("orgId") Long orgId, @Param("userId") Long userId);

    /**
     * The notifications the user has not read, newest first.
     */
    @Query("""
            SELECT n FROM Notification n
            WHERE n.targetOrgId = :orgId
            AND NOT EXISTS (
                SELECT r FROM NotificationReadReceipt r
                WHERE r.notification = n AND r.userId = :userId)
            ORDER BY n.createdAt DESC, n.notificationId DESC
            """)
    List<Notification> findUnreadForUserInOrg(@Param("orgId") Long orgId,
                                               @Param("userId") Long userId,
                                               Pageable pageable);

    /**
     * Look a notification up within an organisation.
     * <p>
     * Used to check that a user may only act on notifications that belong to
     * the organisation they are currently acting in.
     */
    @Query("SELECT n FROM Notification n WHERE n.notificationId = :notificationId AND n.targetOrgId = :orgId")
    Optional<Notification> findByNotificationIdAndTargetOrgId(@Param("notificationId") Long notificationId,
                                                              @Param("orgId") Long orgId);

    /**
     * Notifications created before the given instant, used by the retention
     * job.
     */
    @Query("SELECT n FROM Notification n WHERE n.createdAt < :cutoff")
    List<Notification> findCreatedBefore(@Param("cutoff") Instant cutoff);
}
