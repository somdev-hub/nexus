package com.nexus.hr.model.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.nexus.hr.config.StringEncryptionConverter;
import com.nexus.hr.model.enums.BankAccountType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

@Entity
@Getter
@Setter
@EqualsAndHashCode(exclude = {"compensation"})
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

    @Convert(converter = StringEncryptionConverter.class)
    @Column(columnDefinition = "TEXT")
    private String panNumber;

    @ManyToOne
    @JoinColumn(name = "compensation_id")
    @JsonBackReference("compensation-bankRecords")
    private Compensation compensation;
}
