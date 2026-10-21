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
 * How a notification is addressed.
 * <p>
 * Only organisation addressing is active today. The other modes are in the
 * catalogue so that user-level and authority-level addressing can be switched
 * on by inserting a row rather than by releasing code.
 */
@Entity
@Table(name = "t_notification_addressing_type_master", schema = "cms")
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class NotificationAddressingTypeMaster extends NotificationMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_addressing_type_master_id")
    private Long notificationAddressingTypeMasterId;

    /**
     * The addressing code, e.g. {@code ORG}.
     */
    @Column(name = "notification_addressing_type", nullable = false, unique = true, length = 40)
    private String notificationAddressingType;
}
