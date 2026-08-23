package com.nexus.pms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for Razorpay payment gateway.
 * Properties are loaded from application.properties or environment variables.
 * 
 * Usage in application.properties:
 * razorpay.key-id=${RAZORPAY_KEY_ID}
 * razorpay.key-secret=${RAZORPAY_KEY_SECRET}
 * razorpay.webhook-secret=${RAZORPAY_WEBHOOK_SECRET}
 */
@Component
@ConfigurationProperties(prefix = "razorpay")
@Getter
@Setter
public class RazorpayProperties {
    /**
     * Razorpay API Key ID
     * Get from: https://dashboard.razorpay.com/app/settings/api-keys
     */
    private String keyId;

    /**
     * Razorpay API Key Secret
     * Get from: https://dashboard.razorpay.com/app/settings/api-keys
     */
    private String keySecret;

    /**
     * Razorpay Webhook Secret
     * Get from: https://dashboard.razorpay.com/app/webhooks
     * Used to verify webhook signature authenticity
     */
    private String webhookSecret;

    /**
     * Convenience getter for keyId (for compatibility)
     */
    public String getRazorpayKeyId() {
        return this.keyId;
    }

    /**
     * Convenience getter for keySecret (for compatibility)
     */
    public String getRazorpayKeySecret() {
        return this.keySecret;
    }

    /**
     * Convenience getter for webhookSecret (for compatibility)
     */
    public String getRazorpayWebhookSecret() {
        return this.webhookSecret;
    }
}
