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
     * <p>
     * The transition itself is carried in the event facts
     * ({@code previousStatus}, {@code newStatus}) and is turned into a
     * notification by the notification policy registry.
     */
    PURCHASE_ORDER_STATUS_CHANGED
}
