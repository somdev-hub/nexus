package com.nexus.pms.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.nexus.pms.model.enums.PaymentStatus;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "t_merchant_members", schema = "pms")
public class MerchantMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long merchantMemberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    private Long sourceMemberId;

    private String name;
    private String email;

    private String bankAccountNumber;

    private String bankAccountName;

    private String bankName;

    private String ifscCode;

    private String bankAccountType;

    private Double totalAmountReceivable;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

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