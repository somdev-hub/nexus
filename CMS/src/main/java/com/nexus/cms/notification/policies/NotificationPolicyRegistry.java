package com.nexus.cms.notification.policies;

import java.util.Optional;

/**
 * Looks up the policy for a business event.
 * <p>
 * The notification projector depends on this interface and never on an
 * implementation, so the rules can be moved out of code — into a table, or a
 * remote configuration service — without the projector changing at all.
 */
public interface NotificationPolicyRegistry {

    /**
     * Find the policy for a status transition of a given event type.
     *
     * @param eventType      the kind of domain event (e.g. PURCHASE_ORDER_STATUS_CHANGED)
     * @param previousStatus the status the entity left, may be null
     * @param newStatus      the status the entity moved to
     * @return the policy, or empty when the transition is not worth notifying
     *         anybody about
     */
    Optional<NotificationPolicy> findForTransition(String eventType, String previousStatus, String newStatus);
}