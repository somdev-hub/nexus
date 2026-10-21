package com.nexus.cms.notification.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.sql.Timestamp;

/**
 * A user's record of having read a notification.
 * <p>
 * Read state is per user, not per organisation. When a notification is
 * broadcast to an organisation, one user marking it read must not clear it
 * for everybody else, so the read state lives in its own table.
 * <p>
 * It is also the seam that user-level addressing will need: the same table
 * answers "has this user seen this notification" regardless of how the
 * notification was addressed.
 */
@Entity
@Data
@Table(name = "t_notification_reads", schema = "cms",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_read", columnNames = { "notification_id", "user_id" }))
public class NotificationReadReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationReadId;

    /**
     * The notification that was read.
     */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    /**
     * The user who read it.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * When it was read.
     */
    @Column(name = "read_at", nullable = false)
    private Timestamp readAt;
}
