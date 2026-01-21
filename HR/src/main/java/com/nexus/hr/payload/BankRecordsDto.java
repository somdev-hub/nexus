package com.nexus.hr.payload;

import com.nexus.hr.model.enums.BankAccountType;
import lombok.Data;

@Data
public class BankRecordsDto {
    private String bankName;

    private String accountHolderName;

    private String accountNumber;

    private String ifscCode;

    private BankAccountType accountType;

    private String branchAddress;
}
