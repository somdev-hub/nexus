package com.nexus.pms.model.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.nexus.pms.model.enums.ClientPaymentTypeRecipient;
import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Entity
@Table(name = "t_client_payment_types", schema = "pms")
public class ClientPaymentTypes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long clientPaymentTypeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_master_id")
    @JsonBackReference
    private ClientMaster clientMaster;

    private String clientPaymentTypeName;

    private ClientPaymentTypeRecipient recipient;

    private String description;

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