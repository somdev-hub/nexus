package com.nexus.iam.dto;

import lombok.Data;

@Data
public class BankRecordsDto {
    private String bankName;

    private String accountHolderName;

    private String accountNumber;

    private String ifscCode;

    private String accountType;

    private String branchAddress;

    private String panNumber;
}
