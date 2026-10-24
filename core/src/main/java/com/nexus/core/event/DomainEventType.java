package com.nexus.core.event;

/**
 * The kinds of domain event the notification subsystem understands.
 * <p>
 * The enum is the contract between the producer ({@code nexus/core}) and the
 * consumer ({@code nexus/CMS}). Adding a value is a backwards-compatible
 * change; renaming or removing one requires a {@code schemaVersion} bump.
 * <p>
 * The consumer keeps the catalogue in a master table, so a value can be
 * added or deactivated without a release of the producer — but the producer
 * must still know what it is producing.
 */
public enum DomainEventType {

    /**
     * A purchase order changed status.
     */
    PURCHASE_ORDER_STATUS_CHANGED,

    /**
     * A quotation changed status.
     */
    QUOTATION_STATUS_CHANGED,

    /**
     * A partnership changed status.
     */
    PARTNERSHIP_STATUS_CHANGED,

    /**
     * An invoice changed status.
     */
    INVOICE_STATUS_CHANGED,

    /**
     * A payment changed status.
     */
    PAYMENT_STATUS_CHANGED,

    /**
     * A shipment (logistics) changed status.
     * <p>
     * Covers pickup, in-transit, delivery, and exception transitions.
     * Both supplier and retailer are notified.
     */
    SHIPMENT_STATUS_CHANGED,

    /**
     * A goods receipt changed status.
     * <p>
     * When sent to supplier, the supplier is notified.
     */
    GOODS_RECEIPT_STATUS_CHANGED
}