package com.nexus.core.event.outbox;

/**
 * The lifecycle of an outbox row.
 */
public enum OutboxStatus {

    /**
     * Waiting to be published.
     */
    PENDING,

    /**
     * Published successfully.
     */
    PUBLISHED,

    /**
     * The relay gave up after the configured number of attempts.
     */
    FAILED
}
