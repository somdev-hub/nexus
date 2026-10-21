package com.nexus.cms.notification.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * The kinds of payload carried on the private notification queue.
 * <p>
 * The browser keeps one subscription to the private queue and switches on
 * this value.
 */
@Entity
@Table(name = "t_notification_push_type_master", schema = "cms")
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class NotificationPushTypeMaster extends NotificationMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_push_type_master_id")
    private Long notificationPushTypeMasterId;

    /**
     * The push type code, e.g. {@code MESSAGE}.
     */
    @Column(name = "notification_push_type", nullable = false, unique = true, length = 40)
    private String notificationPushType;
}
