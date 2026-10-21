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
 * The lifecycle of an event that could not be processed.
 */
@Entity
@Table(name = "t_notification_failure_status_master", schema = "cms")
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class NotificationFailureStatusMaster extends NotificationMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_failure_status_master_id")
    private Long notificationFailureStatusMasterId;

    /**
     * The failure status code, e.g. {@code FAILED}.
     */
    @Column(name = "notification_failure_status", nullable = false, unique = true, length = 40)
    private String notificationFailureStatus;
}
