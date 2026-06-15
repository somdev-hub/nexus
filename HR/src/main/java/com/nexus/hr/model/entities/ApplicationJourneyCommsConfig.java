package com.nexus.hr.model.entities;

import com.nexus.hr.model.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Table(name = "t_application_journey_comms_config", schema = "hr")
@Entity
@Data
public class ApplicationJourneyCommsConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long applicationJourneyCommsConfigId;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus applicationStatus;

    @ManyToOne
    @JoinColumn(name = "hr_comms_config_hr_comms_config_id")
    private HrCommsConfig hrCommsConfig;

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
