package com.nexus.cms.notification.service.interfaces;

import java.util.Set;

/**
 * Answers "is this value still allowed?" using the master tables.
 * <p>
 * The catalogue lives in the database, so a new event type, severity or
 * addressing mode is an {@code INSERT} and a value that should no longer be
 * used is deactivated by setting {@code is_active} to false — neither needs a
 * release.
 */
public interface NotificationMasterDataService {

    /**
     * Whether the event type is known and active.
     */
    boolean isActiveEventType(String code);

    /**
     * Whether the severity is known and active.
     */
    boolean isActiveSeverity(String code);

    /**
     * Whether the addressing type is known and active.
     */
    boolean isActiveAddressingType(String code);

    /**
     * Whether the address-to rule is known and active.
     */
    boolean isActiveAddressTo(String code);

    /**
     * Whether the failure status is known and active.
     */
    boolean isActiveFailureStatus(String code);

    /**
     * Whether the push type is known and active.
     */
    boolean isActivePushType(String code);

    /**
     * Every active event type.
     */
    Set<String> activeEventTypes();
}
