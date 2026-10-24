package com.nexus.cms.notification.service.implementations;

import com.nexus.cms.notification.entities.NotificationPolicy;
import com.nexus.cms.notification.properties.NotificationMasterCodes;
import com.nexus.cms.notification.repositories.NotificationPolicyRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

/**
 * Seeds the notification policy table with the default rules on first start-up.
 * <p>
 * Idempotent: a rule that already exists is left alone.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 200)
public class NotificationPolicySeeder implements ApplicationRunner {

    private final NotificationPolicyRepo repo;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed("PURCHASE_ORDER_STATUS_CHANGED", "APPROVED", "SENT_TO_SUPPLIER",
                NotificationMasterCodes.SEVERITY_INFO, "ORG", "COUNTERPARTY",
                "New order {poNumber} to acknowledge",
                "Order {poNumber} is waiting for you to acknowledge it. Requested delivery {expectedDeliveryDate}.");

        seed("PURCHASE_ORDER_STATUS_CHANGED", "SENT_TO_SUPPLIER", "ACKNOWLEDGED",
                NotificationMasterCodes.SEVERITY_INFO, "ORG", "COUNTERPARTY",
                "Order {poNumber} acknowledged",
                "{supplierName} acknowledged order {poNumber} and confirmed delivery on {confirmedDeliveryDate}.");

        seedTarget("PURCHASE_ORDER_STATUS_CHANGED", "REJECTED",
                NotificationMasterCodes.SEVERITY_WARNING, "ORG", "COUNTERPARTY",
                "Order {poNumber} rejected", "Order {poNumber} was rejected: {rejectionReason}");

        seedTarget("PURCHASE_ORDER_STATUS_CHANGED", "CANCELLED",
                NotificationMasterCodes.SEVERITY_WARNING, "ORG", "COUNTERPARTY",
                "Order {poNumber} cancelled", "Order {poNumber} was cancelled.");

        seedTarget("PURCHASE_ORDER_STATUS_CHANGED", "PARTIALLY_RECEIVED",
                NotificationMasterCodes.SEVERITY_INFO, "ORG", "COUNTERPARTY",
                "Order {poNumber} partially received", "Part of order {poNumber} has been received.");

        seedTarget("PURCHASE_ORDER_STATUS_CHANGED", "RECEIVED",
                NotificationMasterCodes.SEVERITY_INFO, "ORG", "COUNTERPARTY",
                "Order {poNumber} received", "Order {poNumber} has been received in full.");

        // Quotations
        seedTarget("QUOTATION_STATUS_CHANGED", "ACCEPTED",
                NotificationMasterCodes.SEVERITY_INFO, "ORG", "COUNTERPARTY",
                "Quotation {quotationNumber} accepted", "Quotation {quotationNumber} has been accepted.");

        seedTarget("QUOTATION_STATUS_CHANGED", "REJECTED",
                NotificationMasterCodes.SEVERITY_WARNING, "ORG", "COUNTERPARTY",
                "Quotation {quotationNumber} rejected", "Quotation {quotationNumber} was rejected.");

        seedTarget("QUOTATION_STATUS_CHANGED", "EXPIRED",
                NotificationMasterCodes.SEVERITY_WARNING, "ORG", "COUNTERPARTY",
                "Quotation {quotationNumber} expired", "Quotation {quotationNumber} has expired.");

        // Partnerships
        seedTarget("PARTNERSHIP_STATUS_CHANGED", "ACTIVE",
                NotificationMasterCodes.SEVERITY_INFO, "ORG", "COUNTERPARTY",
                "Partnership activated", "Your partnership with {secondaryOrgName} is now active.");

        seedTarget("PARTNERSHIP_STATUS_CHANGED", "SUSPENDED",
                NotificationMasterCodes.SEVERITY_WARNING, "ORG", "COUNTERPARTY",
                "Partnership suspended", "Your partnership has been suspended.");

        seedTarget("PARTNERSHIP_STATUS_CHANGED", "TERMINATED",
                NotificationMasterCodes.SEVERITY_WARNING, "ORG", "COUNTERPARTY",
                "Partnership terminated", "Your partnership has been terminated.");

        seedTarget("PARTNERSHIP_STATUS_CHANGED", "REJECTED",
                NotificationMasterCodes.SEVERITY_WARNING, "ORG", "COUNTERPARTY",
                "Partnership rejected", "Your partnership request was rejected.");

        // Invoices
        seedTarget("INVOICE_STATUS_CHANGED", "APPROVED",
                NotificationMasterCodes.SEVERITY_INFO, "ORG", "COUNTERPARTY",
                "Invoice {invoiceNumber} approved", "Invoice {invoiceNumber} has been approved.");

        seedTarget("INVOICE_STATUS_CHANGED", "REJECTED",
                NotificationMasterCodes.SEVERITY_WARNING, "ORG", "COUNTERPARTY",
                "Invoice {invoiceNumber} rejected", "Invoice {invoiceNumber} was rejected.");

        seedTarget("INVOICE_STATUS_CHANGED", "PAID",
                NotificationMasterCodes.SEVERITY_INFO, "ORG", "COUNTERPARTY",
                "Invoice {invoiceNumber} paid", "Invoice {invoiceNumber} has been paid in full.");

        seedTarget("INVOICE_STATUS_CHANGED", "CANCELLED",
                NotificationMasterCodes.SEVERITY_WARNING, "ORG", "COUNTERPARTY",
                "Invoice {invoiceNumber} cancelled", "Invoice {invoiceNumber} was cancelled.");

        // Shipments (logistics)
        seedTarget("SHIPMENT_STATUS_CHANGED", "BOOKED",
                NotificationMasterCodes.SEVERITY_INFO, "ORG", "COUNTERPARTY",
                "Shipment {shipmentNumber} handed over",
                "Shipment {shipmentNumber} has been handed over to logistics for pickup and delivery. Tracking: {trackingNumber}.");

        seedTarget("SHIPMENT_STATUS_CHANGED", "ASSIGNED",
                NotificationMasterCodes.SEVERITY_INFO, "ORG", "COUNTERPARTY",
                "Shipment {shipmentNumber} assigned", "Shipment {shipmentNumber} has been assigned to a carrier.");

        seedTarget("SHIPMENT_STATUS_CHANGED", "PICKED_UP",
                NotificationMasterCodes.SEVERITY_INFO, "ORG", "COUNTERPARTY",
                "Shipment {shipmentNumber} picked up",
                "Shipment {shipmentNumber} has been picked up by the carrier. Tracking: {trackingNumber}.");

        seedTarget("SHIPMENT_STATUS_CHANGED", "IN_TRANSIT",
                NotificationMasterCodes.SEVERITY_INFO, "ORG", "COUNTERPARTY",
                "Shipment {shipmentNumber} in transit", "Shipment {shipmentNumber} is in transit.");

        seedTarget("SHIPMENT_STATUS_CHANGED", "DELIVERED",
                NotificationMasterCodes.SEVERITY_INFO, "ORG", "COUNTERPARTY",
                "Shipment {shipmentNumber} delivered", "Shipment {shipmentNumber} has been delivered.");

        seedTarget("SHIPMENT_STATUS_CHANGED", "PARTIALLY_DELIVERED",
                NotificationMasterCodes.SEVERITY_WARNING, "ORG", "COUNTERPARTY",
                "Shipment {shipmentNumber} partially delivered", "Shipment {shipmentNumber} was partially delivered.");

        seedTarget("SHIPMENT_STATUS_CHANGED", "EXCEPTION",
                NotificationMasterCodes.SEVERITY_CRITICAL, "ORG", "COUNTERPARTY",
                "Shipment {shipmentNumber} exception", "An exception occurred for shipment {shipmentNumber}.");

        seedTarget("SHIPMENT_STATUS_CHANGED", "CANCELLED",
                NotificationMasterCodes.SEVERITY_WARNING, "ORG", "COUNTERPARTY",
                "Shipment {shipmentNumber} cancelled", "Shipment {shipmentNumber} was cancelled.");

        // Goods receipts
        seedTarget("GOODS_RECEIPT_STATUS_CHANGED", "SENT_TO_SUPPLIER",
                NotificationMasterCodes.SEVERITY_INFO, "ORG", "COUNTERPARTY",
                "Goods receipt {grNumber} sent to you", "A goods receipt has been sent to you for review.");

        seedTarget("GOODS_RECEIPT_STATUS_CHANGED", "RECEIVED",
                NotificationMasterCodes.SEVERITY_INFO, "ORG", "COUNTERPARTY",
                "Goods receipt {grNumber} received", "Goods receipt {grNumber} has been received.");

        seedTarget("GOODS_RECEIPT_STATUS_CHANGED", "RETURNED",
                NotificationMasterCodes.SEVERITY_WARNING, "ORG", "COUNTERPARTY",
                "Goods receipt {grNumber} returned", "Goods receipt {grNumber} was returned.");

        seedTarget("GOODS_RECEIPT_STATUS_CHANGED", "REJECTED",
                NotificationMasterCodes.SEVERITY_WARNING, "ORG", "COUNTERPARTY",
                "Goods receipt {grNumber} rejected", "Goods receipt {grNumber} was rejected.");

        seedTarget("GOODS_RECEIPT_STATUS_CHANGED", "CANCELLED",
                NotificationMasterCodes.SEVERITY_WARNING, "ORG", "COUNTERPARTY",
                "Goods receipt {grNumber} cancelled", "Goods receipt {grNumber} was cancelled.");

        // Payments
        seedTarget("PAYMENT_STATUS_CHANGED", "COMPLETED",
                NotificationMasterCodes.SEVERITY_INFO, "ORG", "COUNTERPARTY",
                "Payment completed", "A payment has been completed.");

        seedTarget("PAYMENT_STATUS_CHANGED", "FAILED",
                NotificationMasterCodes.SEVERITY_CRITICAL, "ORG", "COUNTERPARTY",
                "Payment failed", "A payment has failed.");

        seedTarget("PAYMENT_STATUS_CHANGED", "CANCELLED",
                NotificationMasterCodes.SEVERITY_WARNING, "ORG", "COUNTERPARTY",
                "Payment cancelled", "A payment was cancelled.");

        seedTarget("PAYMENT_STATUS_CHANGED", "REFUNDED",
                NotificationMasterCodes.SEVERITY_INFO, "ORG", "COUNTERPARTY",
                "Payment refunded", "A payment has been refunded.");

        log.info("Notification policy seeder completed");
    }

    private void seed(String eventType, String previousStatus, String newStatus,
                      String severity, String addressing, String addressTo,
                      String title, String body) {
        // Check if already exists
        Optional<NotificationPolicy> existing = repo.findByEventTypeAndPreviousStatusAndNewStatusAndAddressTo(
                eventType, previousStatus, newStatus, addressTo);
        if (existing.isPresent()) return;

        NotificationPolicy p = new NotificationPolicy();
        p.setEventType(eventType);
        p.setPreviousStatus(previousStatus);
        p.setNewStatus(newStatus);
        p.setSeverity(severity);
        p.setAddressing(addressing);
        p.setAddressTo(addressTo);
        p.setTitleTemplate(title);
        p.setBodyTemplate(body);
        p.setIsActive(true);
        Timestamp now = Timestamp.from(Instant.now());
        p.setCreatedAt(now);
        p.setCreatedBy("notification-policy-seeder");
        p.setUpdatedAt(now);
        repo.save(p);
    }

    private void seedTarget(String eventType, String newStatus,
                            String severity, String addressing, String addressTo,
                            String title, String body) {
        seed(eventType, null, newStatus, severity, addressing, addressTo, title, body);
    }
}