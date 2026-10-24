package com.nexus.cms.notification.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * A notification rule — the master control for what message is sent, to whom,
 * and with what severity when an entity changes status.
 * <p>
 * Each row represents one rule: "when entity of type X transitions to status Y,
 * send severity Z message to stakeholder W". Multiple rows can exist for the
 * same transition with different {@code addressTo} values, allowing multiple
 * stakeholders to be notified.
 * <p>
 * The rules are stored in the database so a super-admin can tune wording,
 * severity, and addressing without a code release. The {@link
 * com.nexus.cms.notification.policies.DatabaseNotificationPolicyRegistry}
 * caches them in memory and refreshes on every CRUD operation.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "t_notification_policy", schema = "cms")
public class NotificationPolicy extends com.nexus.cms.notification.entities.NotificationMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long policyId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "previous_status", length = 60)
    private String previousStatus;

    @Column(name = "new_status", nullable = false, length = 60)
    private String newStatus;

    @Column(name = "severity", nullable = false, length = 40)
    private String severity;

    @Column(name = "addressing", nullable = false, length = 40)
    private String addressing;

    @Column(name = "address_to", nullable = false, length = 40)
    private String addressTo;

    @Column(name = "title_template", nullable = false, length = 500)
    private String titleTemplate;

    @Column(name = "body_template", length = 2000)
    private String bodyTemplate;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}