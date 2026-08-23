package com.nexus.pms.service.implementations;

import org.springframework.stereotype.Service;

import com.nexus.pms.service.interfaces.BankTransferService;

import lombok.extern.slf4j.Slf4j;

/**
 * Mock implementation of BankTransferService for testing/demo.
 * Simulates real bank transfer behavior without actual bank integration.
 * 
 * In production, this would:
 * - Call actual bank APIs (HDFC, ICICI, State Bank, etc.)
 * - Use NEFT/RTGS/IMPS protocols
 * - Handle real bank webhooks for confirmation
 */
@Service
@Slf4j
public class MockBankTransferService implements BankTransferService {

    @Override
    public BankTransferResult initiateTransfer(
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
            String transactionRef) {

        log.info("Mock bank transfer initiated:");
        log.info("  From: {} ({}) - {}", senderAccountHolder, senderBankName, senderAccountNumber);
        log.info("  To: {} ({}) - {}", recipientAccountHolder, recipientBankName, recipientAccountNumber);
        log.info("  Amount: {} {}", amount, currency);
        log.info("  Reference: {}", transactionRef);

        // Validate input
        if (senderAccountNumber == null || recipientAccountNumber == null || amount <= 0) {
            log.error("Invalid transfer parameters");
            return new BankTransferResult(
                    null,
                    "FAILED",
                    amount,
                    "Invalid transfer parameters");
        }

        // Simulate success/failure (90% success rate for demo)
        boolean success = true;

        if (success) {
            // Generate mock bank transaction ID (real banks return something like:
            // NEFT2024040512345678)
            String bankTransactionId = generateMockBankTransactionId();

            log.info("Mock bank transfer successful: {}", bankTransactionId);

            return new BankTransferResult(
                    bankTransactionId,
                    "INITIATED", // Will move to PROCESSING then COMPLETED
                    amount);
        } else {
            // Simulate occasional failures
            log.warn("Mock bank transfer failed - simulated failure");
            return new BankTransferResult(
                    null,
                    "FAILED",
                    amount,
                    "Insufficient funds in sender account");
        }
    }

    @Override
    public BankTransferResult checkTransferStatus(String transactionId) {
        if (transactionId == null) {
            log.warn("Invalid transaction ID");
            return new BankTransferResult(transactionId, "FAILED", 0.0, "Transaction not found");
        }

        log.info("Checking status for bank transaction: {}", transactionId);

        // Simulate different statuses based on time
        // In real scenario, you'd query bank API
        long currentTime = System.currentTimeMillis();
        long transactionTime = extractTimestampFromTransactionId(transactionId);
        long elapsed = currentTime - transactionTime;

        String status;
        if (elapsed < 10000) {
            status = "INITIATED";
        } else if (elapsed < 30000) {
            status = "PROCESSING";
        } else {
            status = "COMPLETED"; // Assume all old transactions are completed
        }

        log.info("Bank transaction {} status: {}", transactionId, status);

        return new BankTransferResult(
                transactionId,
                status,
                0.0 // Amount not relevant for status check
        );
    }

    /**
     * Generate a mock bank transaction ID that looks realistic.
     * Format: NEFT + Date/Time + Random
     */
    private String generateMockBankTransactionId() {
        long timestamp = System.currentTimeMillis();
        String timeString = String.format("%013d", timestamp);
        String randomSuffix = String.format("%06d", (int) (Math.random() * 999999));
        return "NEFT" + timeString.substring(0, 10) + randomSuffix;
    }

    /**
     * Extract timestamp from transaction ID for status simulation.
     */
    private long extractTimestampFromTransactionId(String transactionId) {
        if (transactionId == null || transactionId.length() < 14) {
            return System.currentTimeMillis();
        }
        try {
            String timeString = transactionId.substring(4, 14);
            return Long.parseLong(timeString) * 1000000; // Reconstruct approximate timestamp
        } catch (NumberFormatException e) {
            return System.currentTimeMillis();
        }
    }
}
