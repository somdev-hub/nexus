package com.nexus.pms.model.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "t_client_master", schema = "pms")
@Data
@ToString(exclude = "clientPaymentTypes")
public class ClientMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long clientMasterId;

    private String clientCode;

    private String clientName;

    @OneToMany(mappedBy = "clientMaster", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<ClientPaymentTypes> clientPaymentTypes;

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
