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
     * Find the policy for a purchase order status transition.
     *
     * @param previousStatus the status the order left, may be null for a
     *                       newly created order
     * @param newStatus      the status the order moved to
     * @return the policy, or empty when the transition is not worth notifying
     *         anybody about
     */
    Optional<NotificationPolicy> findForTransition(String previousStatus, String newStatus);
}
