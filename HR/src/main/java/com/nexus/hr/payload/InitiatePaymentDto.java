package com.nexus.hr.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class InitiatePaymentDto {

    /**
     * "amount": 50000,
     * "currency": "INR",
     * "description": "Monthly Salary - March 2026",
     * "paymentType": "SALARY",
     * "transactionReference": "SALARY_MARCH_2026_EMP1018",
     * "metadata": "Batch: HR-2026-03",
     * "customer": {
     * "clientMasterId":10,
     * "sourceSystemId":10,
     * "customerName": "ABC Corporation",
     * "customerEmail": "payroll@abccorp.com",
     * "customerPhone": "+91-9876543210",
     * "addressLine1":"Mumbai",
     * "addressLine2":"India",
     * "city":"mumbai",
     * "state":"mahastra",
     * "pinCode":"101010",
     * "country":"india"
     * },
     * "paymentMethod": {
     * "paymentMethod": "NET_BANKING",
     * "bankName": "HDFC Bank",
     * "bankAccountNumber": "9876543210123456",
     * "bankIfscCode": "HDFC0000001",
     * "bankAccountHolderName": "ABC Corporation"
     * },
     * "merchant": {
     * "clientMasterId":10,
     * "sourceSystemId": 10,
     * "merchantOfficialEmail": "hr@nexus.com",
     * "addressLine1": "456 HR Avenue",
     * "city": "Bangalore",
     * "state": "Karnataka",
     * "pinCode": "560001",
     * "country": "India",
     * "merchantMembers": [
     * {
     * "sourceMemberId": 101,
     * "name": "John Doe",
     * "email": "operatorgold69@gmail.com",
     * "bankAccountNumber": "1234567890123456",
     * "bankAccountName": "John Doe",
     * "bankName": "ICICI Bank",
     * "ifscCode": "ICIC0000001",
     * "bankAccountType": "SAVINGS",
     * "totalAmountReceivable": 50000
     * }
     * ]
     * }
     */

    private Double amount;
    private String description;
    private String paymentType;
    private String transactionReference;
    private String currency;
    private String metadata;
    private Customer customer;
    private PaymentMethod paymentMethod;
    private Merchant merchant;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Customer {
        private Long clientMasterId;
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
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethod {
        private String paymentMethod;
        private String bankName;
        private String bankAccountNumber;
        private String bankIfscCode;
        private String bankAccountHolderName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Merchant {
        private Long clientMasterId;
        private Long sourceSystemId;
        private String merchantOfficialEmail;
        private String addressLine1;
        private String city;
        private String state;
        private String pinCode;
        private String country;
        private List<MerchantMember> merchantMembers;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class MerchantMember {
            private Long sourceMemberId;
            private String name;
            private String email;
            private String bankAccountNumber;
            private String bankAccountName;
            private String bankName;
            private String ifscCode;
            private String bankAccountType;
            private Double totalAmountReceivable;
            private PayComponentDto payComponents;
        }
    }
}
