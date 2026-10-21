package com.nexus.cms.notification.properties;

/**
 * The catalogue values the notification subsystem uses in its own code paths.
 * <p>
 * These are <b>not</b> an enum. The master tables in the database hold the
 * catalogue, and this class only names the values the code needs to refer to
 * by name. A new event type, severity or addressing mode is added by
 * inserting a row into the master table — no code change, no release.
 */
public final class NotificationMasterCodes {

    private NotificationMasterCodes() {
    }

    // ── Event types ─────────────────────────────────────────────────────────

    /**
     * A purchase order changed status.
     */
    public static final String EVENT_PURCHASE_ORDER_STATUS_CHANGED = "PURCHASE_ORDER_STATUS_CHANGED";

    // ── Severities ──────────────────────────────────────────────────────────

    /**
     * Ordinary progress.
     */
    public static final String SEVERITY_INFO = "INFO";

    /**
     * Something needs attention but nothing is broken.
     */
    public static final String SEVERITY_WARNING = "WARNING";

    /**
     * Something is wrong.
     */
    public static final String SEVERITY_CRITICAL = "CRITICAL";

    /**
     * A specific person must act. Reserved for authority-based addressing.
     */
    public static final String SEVERITY_ACTION_REQUIRED = "ACTION_REQUIRED";

    // ── Addressing types ───────────────────────────────────────────────────

    /**
     * Broadcast to every connected member of the target organisation.
     */
    public static final String ADDRESSING_ORG = "ORG";

    /**
     * Delivered to a named set of users. Reserved.
     */
    public static final String ADDRESSING_USER = "USER";

    /**
     * Delivered to the users holding a given authority. Reserved.
     */
    public static final String ADDRESSING_AUTHORITY = "AUTHORITY";

    // ── Address-to ──────────────────────────────────────────────────────────

    /**
     * The organisation that did not perform the action.
     */
    public static final String ADDRESS_TO_COUNTERPARTY = "COUNTERPARTY";

    /**
     * The organisation that performed the action.
     */
    public static final String ADDRESS_TO_ACTOR_ORG = "ACTOR_ORG";

    /**
     * The organisation that raised the purchase order.
     */
    public static final String ADDRESS_TO_RETAILER = "RETAILER";

    /**
     * The organisation that fulfils the purchase order.
     */
    public static final String ADDRESS_TO_SUPPLIER = "SUPPLIER";

    // ── Failure statuses ────────────────────────────────────────────────────

    /**
     * The event could not be projected and is waiting to be replayed.
     */
    public static final String FAILURE_STATUS_FAILED = "FAILED";

    /**
     * The event has been republished to the source topic.
     */
    public static final String FAILURE_STATUS_REPLAYED = "REPLAYED";

    /**
     * The event has been given up on.
     */
    public static final String FAILURE_STATUS_PERMANENT = "PERMANENT";

    // ── Push types ──────────────────────────────────────────────────────────

    /**
     * A newly created notification.
     */
    public static final String PUSH_TYPE_MESSAGE = "MESSAGE";

    /**
     * The current inbox snapshot and unread count.
     */
    public static final String PUSH_TYPE_SNAPSHOT = "SNAPSHOT";

    /**
     * Acknowledgement of a read-state change.
     */
    public static final String PUSH_TYPE_ACK = "ACK";
}
