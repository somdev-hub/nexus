package com.nexus.pms.model.enums;

/**
 * Payment type enumeration for different payment scenarios.
 * 
 * Examples:
 * - SALARY: Employee salary payment
 * - REFUND: Payment refund
 * - INVOICE: Invoice payment
 * - SUPPLY: Supplier/vendor payment
 */
public enum PaymentType {
    SALARY("Salary Payment"),
    REFUND("Refund"),
    INVOICE("Invoice Payment"),
    SUPPLY("Supplier Payment"),
    OTHER("Other Payment");

    private final String description;

    PaymentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
