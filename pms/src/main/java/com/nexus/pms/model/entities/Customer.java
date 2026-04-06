package com.nexus.pms.model.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

@Entity
@Table(name = "t_customers", schema = "pms")
@Data
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_master_id", nullable = false)
    private ClientMaster sourceSystem;

    private Long sourceSystemId;

    private String customerName;

    private String customerEmail;

    private String customerPhone;

    private String addressLine1;

    private String addressLine2;

    private String city;

    private String state;

    private String pinCode;

    private String country;

    private Boolean isKycVerified;

    private Boolean isBlocked;

    private Timestamp createdAt;

    private Timestamp updatedAt;

    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        createdAt = new Timestamp(System.currentTimeMillis());
        isActive = true;
        updatedAt = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Timestamp(System.currentTimeMillis());
    }
}
