package com.nexus.hr.model.entities;

import com.nexus.hr.config.StringEncryptionConverter;
import com.nexus.hr.model.enums.BankAccountType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "t_org_account_info", schema = "hr")
@Data
public class OrgAccountInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orgAccountInfoId;

    @Convert(converter = StringEncryptionConverter.class)
    @Column(columnDefinition = "TEXT")
    private String bankAccountNumber; // Encrypted in application

    @Convert(converter = StringEncryptionConverter.class)
    @Column(columnDefinition = "TEXT")
    private String bankAccountName; // Account holder name

    @Convert(converter = StringEncryptionConverter.class)
    @Column(columnDefinition = "TEXT")
    private String bankName;

    @Convert(converter = StringEncryptionConverter.class)
    @Column(columnDefinition = "TEXT")
    private String bankIfscCode;

    @Enumerated(EnumType.STRING)
    private BankAccountType bankAccountType; // SAVINGS, CURRENT

    @Convert(converter = StringEncryptionConverter.class)
    @Column(columnDefinition = "TEXT")
    private String bankAccountBranch;

    @Convert(converter = StringEncryptionConverter.class)
    @Column(columnDefinition = "TEXT")
    private String panNumber;

    private Long orgId;

    private Boolean isActive;

    @CreationTimestamp
    @Column(updatable = false)
    private String createdAt;

    @UpdateTimestamp
    private String updatedAt;

    @PrePersist
    protected void onCreate() {
        isActive = true;
    }
}
