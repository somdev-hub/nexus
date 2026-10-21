package com.nexus.cms.notification.entities;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.sql.Timestamp;

/**
 * The columns every notification master table shares.
 * <p>
 * The master tables replace the Java enums the notification subsystem used to
 * rely on. An enum is a closed set: adding a value means a code change, a
 * compile, and a release, and the value cannot be withdrawn without a data
 * migration. A master table is data: a new event type, severity or addressing
 * mode is an {@code INSERT}, and a value that should no longer be used is
 * deactivated by setting {@code is_active} to false — no release needed.
 * <p>
 * The master tables are seeded on start-up, so a fresh database has a working
 * catalogue without anybody having to run a script.
 */
@MappedSuperclass
@Getter
@Setter
@ToString
@EqualsAndHashCode
public abstract class NotificationMasterEntity {

    /**
     * What the value means, in words meant for a person rather than a
     * program.
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * When the row was created.
     */
    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    /**
     * Who created the row.
     */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    /**
     * Whether the value may still be used. A value that is no longer wanted
     * is deactivated rather than deleted, so that the rows that already
     * reference it still make sense.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * When the row was last changed.
     */
    @Column(name = "updated_at")
    private Timestamp updatedAt;
}
