package com.nexus.hr.model.entities;

import com.nexus.hr.model.enums.CommType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Data
@Table(name = "t_hr_comms_config", schema = "hr")
@Entity
public class HrCommsConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long hrCommsConfigId;

    @Column(unique = true)
    private String commsTriggerPoint;

    @Column(unique = true)
    private String templateName;

    @Enumerated(EnumType.STRING)
    private CommType commType;

    @Column(columnDefinition = "JSONB")
    private String commParams;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        isActive = true;
    }
}
