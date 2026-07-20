package com.nexus.nexusbuddy.model.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Entity
@Table(name = "t_nexus_buddy_logs", schema = "nexusbuddy")
@Data
@NoArgsConstructor
@ToString(exclude = {"clientConfig"})
@EqualsAndHashCode(exclude = {"clientConfig"})
public class NexusBuddyLogs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long nexusBuddyLogId;

    private String requestUrl;

    private String httpMethod;

    private Integer responseStatus;

    @Column(columnDefinition = "JSONB")
    private String request;

    @Column(columnDefinition = "JSONB")
    private String response;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    private Boolean isActive;

    @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "client_config_id")
    @JsonBackReference
    private ClientConfig clientConfig;

    private String toolName;

    @PrePersist
    protected void onCreate() {
        isActive = true;
    }
}
