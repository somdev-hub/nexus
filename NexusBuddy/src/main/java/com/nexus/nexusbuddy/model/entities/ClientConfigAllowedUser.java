package com.nexus.nexusbuddy.model.entities;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Entity
@Table(name = "t_client_config_allowed_user", schema = "nexusbuddy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientConfigAllowedUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allowed_user_id")
    private Long allowedUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_config_id", nullable = false)
    private ClientConfig clientConfig;

    @Column(name = "domain", nullable = false, length = 255)
    private String domain;

    @CreationTimestamp
    @Column(name = "created_on", nullable = false, updatable = false)
    private Timestamp createdOn;

    @UpdateTimestamp
    @Column(name = "updated_on")
    private Timestamp updatedOn;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        isActive = true;
    }
}