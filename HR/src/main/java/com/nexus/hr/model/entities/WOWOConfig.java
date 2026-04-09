package com.nexus.hr.model.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Entity
@Data
@Table(name = "t_wowo_config", schema = "hr")
public class WOWOConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long wowoConfigId;

    @Column(nullable = false, unique = true)
    private String wowoName;

    @Column(nullable = false)
    private Boolean isActive;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;
}
