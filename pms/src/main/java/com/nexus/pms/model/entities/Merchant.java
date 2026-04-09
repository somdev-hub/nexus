package com.nexus.pms.model.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "t_merchants", schema = "pms")
@Data
@ToString(exclude = "merchantMembers")
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long merchantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_master_id", nullable = false)
    private ClientMaster sourceSystem;

    private Long sourceSystemId;

    private String paymentReferenceId;

    private String merchantOfficialEmail;

    private String addressLine1;

    private String addressLine2;

    private String city;

    private String state;

    private String pinCode;

    private String country;

    private String bankAccountNumber;

    private String bankAccountName;

    private String bankName;

    private String ifscCode;

    private String bankAccountType;

    @OneToMany(mappedBy = "merchant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MerchantMember> merchantMembers;

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
