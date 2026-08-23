package com.nexus.pms.exception;

/**
 * Exception thrown when Razorpay API communication fails.
 */
public class RazorpayException extends PaymentException {

    public RazorpayException(String message) {
        super(message, "RAZORPAY_ERROR");
    }

    public RazorpayException(String message, String errorCode) {
        super(message, "RAZORPAY_" + errorCode);
    }

    public RazorpayException(String message, String errorCode, Throwable cause) {
        super(message, "RAZORPAY_" + errorCode, cause.getMessage());
    }
}
