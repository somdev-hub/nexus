package com.nexus.cms.notification.policies;

import com.nexus.cms.notification.properties.NotificationMasterCodes;
import com.nexus.cms.notification.properties.NotificationConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The notification rules, held in code.
 * <p>
 * Policies are now keyed by event type AND status transition, so that the
 * same status string (e.g. CANCELLED) on different entity types resolves
 * to the correct policy.
 */
@Slf4j
// @Component — replaced by DatabaseNotificationPolicyRegistry
public class InMemoryNotificationPolicyRegistry implements NotificationPolicyRegistry {

    /**
     * Rules for a specific transition, keyed {@code eventType|previousStatus->newStatus}.
     */
    private final Map<String, NotificationPolicy> byTransition = new HashMap<>();

    /**
     * Rules that apply whatever the previous status was, keyed by
     * {@code eventType|newStatus}.
     */
    private final Map<String, NotificationPolicy> byTargetStatus = new HashMap<>();

    public InMemoryNotificationPolicyRegistry() {
        // ── Purchase order policies ─────────────────────────────────────────
        String po = NotificationMasterCodes.EVENT_PURCHASE_ORDER_STATUS_CHANGED;

        registerTransition(po,
                NotificationConstants.PO_STATUS_APPROVED,
                NotificationConstants.PO_STATUS_SENT_TO_SUPPLIER,
                NotificationPolicy.of(
                        NotificationMasterCodes.SEVERITY_INFO,
                        "New order {poNumber} to acknowledge",
                        "Order {poNumber} is waiting for you to acknowledge it."
                                + " Requested delivery {expectedDeliveryDate}.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTransition(po,
                NotificationConstants.PO_STATUS_SENT_TO_SUPPLIER,
                NotificationConstants.PO_STATUS_ACKNOWLEDGED,
                NotificationPolicy.of(
                        NotificationMasterCodes.SEVERITY_INFO,
                        "Order {poNumber} acknowledged",
                        "{supplierName} acknowledged order {poNumber}"
                                + " and confirmed delivery on {confirmedDeliveryDate}.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(po, NotificationConstants.PO_STATUS_REJECTED,
                NotificationPolicy.of(
                        NotificationMasterCodes.SEVERITY_WARNING,
                        "Order {poNumber} rejected",
                        "Order {poNumber} was rejected: {rejectionReason}",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(po, NotificationConstants.PO_STATUS_CANCELLED,
                NotificationPolicy.of(
                        NotificationMasterCodes.SEVERITY_WARNING,
                        "Order {poNumber} cancelled",
                        "Order {poNumber} was cancelled.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(po, NotificationConstants.PO_STATUS_PARTIALLY_RECEIVED,
                NotificationPolicy.of(
                        NotificationMasterCodes.SEVERITY_INFO,
                        "Order {poNumber} partially received",
                        "Part of order {poNumber} has been received.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(po, NotificationConstants.PO_STATUS_RECEIVED,
                NotificationPolicy.of(
                        NotificationMasterCodes.SEVERITY_INFO,
                        "Order {poNumber} received",
                        "Order {poNumber} has been received in full.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        // ── Quotation policies ──────────────────────────────────────────────
        String q = NotificationMasterCodes.EVENT_QUOTATION_STATUS_CHANGED;

        registerTarget(q, "ACCEPTED",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_INFO,
                        "Quotation {quotationNumber} accepted",
                        "Quotation {quotationNumber} has been accepted.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(q, "REJECTED",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_WARNING,
                        "Quotation {quotationNumber} rejected",
                        "Quotation {quotationNumber} was rejected.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(q, "EXPIRED",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_WARNING,
                        "Quotation {quotationNumber} expired",
                        "Quotation {quotationNumber} has expired.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        // ── Partnership policies ────────────────────────────────────────────
        String p = NotificationMasterCodes.EVENT_PARTNERSHIP_STATUS_CHANGED;

        registerTarget(p, "ACTIVE",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_INFO,
                        "Partnership activated",
                        "Your partnership with {secondaryOrgName} is now active.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(p, "SUSPENDED",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_WARNING,
                        "Partnership suspended",
                        "Your partnership has been suspended.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(p, "TERMINATED",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_WARNING,
                        "Partnership terminated",
                        "Your partnership has been terminated.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(p, "REJECTED",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_WARNING,
                        "Partnership rejected",
                        "Your partnership request was rejected.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        // ── Invoice policies ────────────────────────────────────────────────
        String inv = NotificationMasterCodes.EVENT_INVOICE_STATUS_CHANGED;

        registerTarget(inv, "APPROVED",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_INFO,
                        "Invoice {invoiceNumber} approved",
                        "Invoice {invoiceNumber} has been approved.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(inv, "REJECTED",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_WARNING,
                        "Invoice {invoiceNumber} rejected",
                        "Invoice {invoiceNumber} was rejected.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(inv, "PAID",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_INFO,
                        "Invoice {invoiceNumber} paid",
                        "Invoice {invoiceNumber} has been paid in full.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(inv, "CANCELLED",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_WARNING,
                        "Invoice {invoiceNumber} cancelled",
                        "Invoice {invoiceNumber} was cancelled.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        // ── Shipment (logistics) policies ───────────────────────────────────
        String ship = NotificationMasterCodes.EVENT_SHIPMENT_STATUS_CHANGED;

        registerTarget(ship, "BOOKED",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_INFO,
                        "Shipment {shipmentNumber} handed over",
                        "Shipment {shipmentNumber} has been handed over to logistics"
                                + " for pickup and delivery. Tracking: {trackingNumber}.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(ship, "ASSIGNED",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_INFO,
                        "Shipment {shipmentNumber} assigned",
                        "Shipment {shipmentNumber} has been assigned to a carrier.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(ship, "PICKED_UP",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_INFO,
                        "Shipment {shipmentNumber} picked up",
                        "Shipment {shipmentNumber} has been picked up by the carrier."
                                + " Tracking: {trackingNumber}.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(ship, "IN_TRANSIT",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_INFO,
                        "Shipment {shipmentNumber} in transit",
                        "Shipment {shipmentNumber} is in transit.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(ship, "DELIVERED",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_INFO,
                        "Shipment {shipmentNumber} delivered",
                        "Shipment {shipmentNumber} has been delivered.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(ship, "PARTIALLY_DELIVERED",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_WARNING,
                        "Shipment {shipmentNumber} partially delivered",
                        "Shipment {shipmentNumber} was partially delivered.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(ship, "EXCEPTION",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_CRITICAL,
                        "Shipment {shipmentNumber} exception",
                        "An exception occurred for shipment {shipmentNumber}.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(ship, "CANCELLED",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_WARNING,
                        "Shipment {shipmentNumber} cancelled",
                        "Shipment {shipmentNumber} was cancelled.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        // ── Goods receipt policies ──────────────────────────────────────────
        String gr = NotificationMasterCodes.EVENT_GOODS_RECEIPT_STATUS_CHANGED;

        registerTarget(gr, "SENT_TO_SUPPLIER",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_INFO,
                        "Goods receipt {grNumber} sent to you",
                        "A goods receipt has been sent to you for review.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(gr, "RECEIVED",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_INFO,
                        "Goods receipt {grNumber} received",
                        "Goods receipt {grNumber} has been received.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(gr, "RETURNED",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_WARNING,
                        "Goods receipt {grNumber} returned",
                        "Goods receipt {grNumber} was returned.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(gr, "REJECTED",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_WARNING,
                        "Goods receipt {grNumber} rejected",
                        "Goods receipt {grNumber} was rejected.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(gr, "CANCELLED",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_WARNING,
                        "Goods receipt {grNumber} cancelled",
                        "Goods receipt {grNumber} was cancelled.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        // ── Payment policies ────────────────────────────────────────────────
        String pay = NotificationMasterCodes.EVENT_PAYMENT_STATUS_CHANGED;

        registerTarget(pay, "COMPLETED",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_INFO,
                        "Payment completed",
                        "A payment has been completed.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(pay, "FAILED",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_CRITICAL,
                        "Payment failed",
                        "A payment has failed.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(pay, "CANCELLED",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_WARNING,
                        "Payment cancelled",
                        "A payment was cancelled.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(pay, "REFUNDED",
                NotificationPolicy.of(NotificationMasterCodes.SEVERITY_INFO,
                        "Payment refunded",
                        "A payment has been refunded.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        log.info("Notification policy registry initialised with {} transition rules and {} target-status rules",
                byTransition.size(), byTargetStatus.size());
    }

    private void registerTransition(String eventType, String previousStatus, String newStatus, NotificationPolicy policy) {
        byTransition.put(transitionKey(eventType, previousStatus, newStatus), policy);
    }

    private void registerTarget(String eventType, String newStatus, NotificationPolicy policy) {
        byTargetStatus.put(targetKey(eventType, newStatus), policy);
    }

    @Override
    public Optional<NotificationPolicy> findForTransition(String eventType, String previousStatus, String newStatus) {
        if (newStatus == null) {
            return Optional.empty();
        }
        // An exact transition wins over a rule that applies to the target
        // status from anywhere.
        if (previousStatus != null && eventType != null) {
            NotificationPolicy exact = byTransition.get(transitionKey(eventType, previousStatus, newStatus));
            if (exact != null) {
                return Optional.of(exact);
            }
        }
        if (eventType != null) {
            NotificationPolicy target = byTargetStatus.get(targetKey(eventType, newStatus));
            if (target != null) {
                return Optional.of(target);
            }
        }
        // Fallback: try without event type for backwards compatibility
        if (previousStatus != null) {
            NotificationPolicy exact = byTransition.get(transitionKey(null, previousStatus, newStatus));
            if (exact != null) {
                return Optional.of(exact);
            }
        }
        return Optional.ofNullable(byTargetStatus.get(targetKey(null, newStatus)));
    }

    private static String transitionKey(String eventType, String previousStatus, String newStatus) {
        return (eventType != null ? eventType : "*") + "|" + previousStatus + "->" + newStatus;
    }

    private static String targetKey(String eventType, String newStatus) {
        return (eventType != null ? eventType : "*") + "|" + newStatus;
    }
}