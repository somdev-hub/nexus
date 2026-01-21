package com.nexus.hr.model.entities;

import com.nexus.hr.config.StringEncryptionConverter;
import com.nexus.hr.model.enums.BankAccountType;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "t_bank_records", schema = "hr")
public class BankRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bankRecordId;

    @Convert(converter = StringEncryptionConverter.class)
    @Column(columnDefinition = "TEXT")
    private String bankName;

    @Convert(converter = StringEncryptionConverter.class)
    @Column(columnDefinition = "TEXT")
    private String accountHolderName;

    @Convert(converter = StringEncryptionConverter.class)
    @Column(columnDefinition = "TEXT")
    private String accountNumber;

    @Convert(converter = StringEncryptionConverter.class)
    @Column(columnDefinition = "TEXT")
    private String ifscCode;

    private BankAccountType accountType;

    @Convert(converter = StringEncryptionConverter.class)
    @Column(columnDefinition = "TEXT")
    private String branchAddress;

    @ManyToOne
    @JoinColumn(name = "compensation_id")
    private Compensation compensation;
}
