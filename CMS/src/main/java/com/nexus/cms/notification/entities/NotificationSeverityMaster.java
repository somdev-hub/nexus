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
 * How prominent a notification is.
 */
@Entity
@Table(name = "t_notification_severity_master", schema = "cms")
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class NotificationSeverityMaster extends NotificationMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_severity_master_id")
    private Long notificationSeverityMasterId;

    /**
     * The severity code, e.g. {@code INFO}.
     */
    @Column(name = "notification_severity", nullable = false, unique = true, length = 40)
    private String notificationSeverity;
}
