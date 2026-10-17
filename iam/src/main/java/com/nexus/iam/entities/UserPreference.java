package com.nexus.iam.entities;

import java.sql.Timestamp;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-user preference row (IAM).
 *
 * <p>
 * Each row stores a single preference value for a user, scoped by a
 * {@code setting_name} (feature group, e.g. {@code appearance}) and a
 * {@code key} (individual option, e.g. {@code theme}). The {@code value}
 * column is {@code jsonb} so a preference can hold a scalar
 * (string/number/boolean) or a richer structure (object/array).
 * </p>
 *
 * <p>
 * The table {@code iam.t_user_preferences} is created/updated by Hibernate
 * ({@code spring.jpa.hibernate.ddl-auto=update}), like the rest of the IAM
 * schema. The {@code value} mapping mirrors {@link Logs} (a raw JSON text
 * String bound to a {@code jsonb} column).
 * </p>
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "t_user_preferences", schema = "iam")
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_preference_id")
    private Long userPreferenceId;

    /** The user this preference belongs to ({@code iam.t_users.id}). */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Feature/group the preference belongs to, e.g. {@code appearance}. */
    @Column(name = "setting_name", nullable = false)
    private String settingName;

    /** Individual preference under the setting, e.g. {@code theme}. */
    @Column(name = "key", nullable = false)
    private String key;

    /**
     * Raw JSON text stored in a {@code jsonb} column, so the same row can hold
     * either a scalar ({@code "neutral"}, {@code 0.625}) or a structure
     * ({@code {"nested":[1,2]}}).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "value", columnDefinition = "jsonb")
    private String value;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @PrePersist
    private void onCreate() {
        if (isActive == null) {
            isActive = true;
        }
    }
}
