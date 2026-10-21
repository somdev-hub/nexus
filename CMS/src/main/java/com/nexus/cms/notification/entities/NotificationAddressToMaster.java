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
 * Which organisation a notification is addressed to.
 */
@Entity
@Table(name = "t_notification_address_to_master", schema = "cms")
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class NotificationAddressToMaster extends NotificationMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_address_to_master_id")
    private Long notificationAddressToMasterId;

    /**
     * The address-to code, e.g. {@code COUNTERPARTY}.
     */
    @Column(name = "notification_address_to", nullable = false, unique = true, length = 40)
    private String notificationAddressTo;
}
