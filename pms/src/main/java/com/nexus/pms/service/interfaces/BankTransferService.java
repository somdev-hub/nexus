package com.nexus.pms.service.interfaces;

/**
 * Service interface for bank transfer operations.
 * Handles direct bank-to-bank transfers via NEFT/RTGS/IMPS.
 * 
 * This is separate from Razorpay - used when both customer and merchant
 * have bank account details already available.
 */
public interface BankTransferService {

    /**
     * Initiate a direct bank transfer between accounts.
     * 
     * @param senderAccountNumber    Sender's bank account number
     * @param senderBankName         Sender's bank name
     * @param senderIfscCode         Sender's IFSC code
     * @param senderAccountHolder    Sender's account holder name
     * @param recipientAccountNumber Recipient's bank account number
     * @param recipientBankName      Recipient's bank name
     * @param recipientIfscCode      Recipient's IFSC code
     * @param recipientAccountHolder Recipient's account holder name
     * @param amount                 Transfer amount
     * @param currency               Currency code (e.g., "INR")
     * @param transactionRef         Unique transaction reference
     * @return BankTransferResult with transaction ID and status
     */
    BankTransferResult initiateTransfer(
            String senderAccountNumber,
            String senderBankName,
            String senderIfscCode,
            String senderAccountHolder,
            String recipientAccountNumber,
            String recipientBankName,
            String recipientIfscCode,
            String recipientAccountHolder,
            Double amount,
            String currency,
            String transactionRef);

    /**
     * Check the status of a bank transfer.
     * 
     * @param transactionId Bank transaction ID returned by initiateTransfer
     * @return BankTransferResult with current status
     */
    BankTransferResult checkTransferStatus(String transactionId);

    /**
     * Result of bank transfer operation.
     */
    class BankTransferResult {
        private String transactionId; // Bank-generated transaction ID
        private String status; // INITIATED, PROCESSING, COMPLETED, FAILED
        private Double amount;
        private String errorMessage;
        private Long timestamp;

        public BankTransferResult(String transactionId, String status, Double amount) {
            this.transactionId = transactionId;
            this.status = status;
            this.amount = amount;
            this.timestamp = System.currentTimeMillis();
        }

        public BankTransferResult(String transactionId, String status, Double amount, String errorMessage) {
            this(transactionId, status, amount);
            this.errorMessage = errorMessage;
        }

        // Getters and setters
        public String getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(String transactionId) {
            this.transactionId = transactionId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
