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
 * The kinds of domain event the notification subsystem understands.
 * <p>
 * The code is the wire value carried on a domain event, so it is part of the
 * contract with the producer.
 */
@Entity
@Table(name = "t_notification_event_type_master", schema = "cms")
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class NotificationEventTypeMaster extends NotificationMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_event_type_master_id")
    private Long notificationEventTypeMasterId;

    /**
     * The event type code, e.g. {@code PURCHASE_ORDER_STATUS_CHANGED}.
     */
    @Column(name = "notification_event_type", nullable = false, unique = true, length = 100)
    private String notificationEventType;
}
