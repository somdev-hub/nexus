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
 * The rules are business-critical: they decide what a user sees and how
 * urgent it is. Keeping them in code means a mistake is a compile error
 * rather than a broken notification for every organisation at once, and it
 * means the rules are reviewed and tested like any other logic.
 * <p>
 * The registry is behind {@link NotificationPolicyRegistry}, so swapping in a
 * table-backed implementation later touches nothing else. The trigger for
 * doing that is "when non-developers need to tune the wording or the
 * severity without a release".
 * <p>
 * A placeholder with no matching fact is removed when the template is
 * rendered, so a missing fact never leaves a stray {@code {token}} in front
 * of a user.
 */
@Slf4j
@Component
public class InMemoryNotificationPolicyRegistry implements NotificationPolicyRegistry {

    /**
     * Rules for a specific transition, keyed {@code previousStatus->newStatus}.
     */
    private final Map<String, NotificationPolicy> byTransition = new HashMap<>();

    /**
     * Rules that apply whatever the previous status was, keyed by the new
     * status. Used for the outcomes that can be reached from many places.
     */
    private final Map<String, NotificationPolicy> byTargetStatus = new HashMap<>();

    public InMemoryNotificationPolicyRegistry() {
        registerTransition(
                NotificationConstants.PO_STATUS_APPROVED,
                NotificationConstants.PO_STATUS_SENT_TO_SUPPLIER,
                NotificationPolicy.of(
                        NotificationMasterCodes.SEVERITY_INFO,
                        "New order {poNumber} to acknowledge",
                        "Order {poNumber} is waiting for you to acknowledge it."
                                + " Requested delivery {expectedDeliveryDate}.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTransition(
                NotificationConstants.PO_STATUS_SENT_TO_SUPPLIER,
                NotificationConstants.PO_STATUS_ACKNOWLEDGED,
                NotificationPolicy.of(
                        NotificationMasterCodes.SEVERITY_INFO,
                        "Order {poNumber} acknowledged",
                        "{supplierName} acknowledged order {poNumber}"
                                + " and confirmed delivery on {confirmedDeliveryDate}.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(
                NotificationConstants.PO_STATUS_REJECTED,
                NotificationPolicy.of(
                        NotificationMasterCodes.SEVERITY_WARNING,
                        "Order {poNumber} rejected",
                        "Order {poNumber} was rejected: {rejectionReason}",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(
                NotificationConstants.PO_STATUS_CANCELLED,
                NotificationPolicy.of(
                        NotificationMasterCodes.SEVERITY_WARNING,
                        "Order {poNumber} cancelled",
                        "Order {poNumber} was cancelled.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(
                NotificationConstants.PO_STATUS_PARTIALLY_RECEIVED,
                NotificationPolicy.of(
                        NotificationMasterCodes.SEVERITY_INFO,
                        "Order {poNumber} partially received",
                        "Part of order {poNumber} has been received.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        registerTarget(
                NotificationConstants.PO_STATUS_RECEIVED,
                NotificationPolicy.of(
                        NotificationMasterCodes.SEVERITY_INFO,
                        "Order {poNumber} received",
                        "Order {poNumber} has been received in full.",
                        NotificationMasterCodes.ADDRESS_TO_COUNTERPARTY));

        log.info("Notification policy registry initialised with {} transition rules and {} target-status rules",
                byTransition.size(), byTargetStatus.size());
    }

    private void registerTransition(String previousStatus, String newStatus, NotificationPolicy policy) {
        byTransition.put(transitionKey(previousStatus, newStatus), policy);
    }

    private void registerTarget(String newStatus, NotificationPolicy policy) {
        byTargetStatus.put(newStatus, policy);
    }

    @Override
    public Optional<NotificationPolicy> findForTransition(String previousStatus, String newStatus) {
        if (newStatus == null) {
            return Optional.empty();
        }
        // An exact transition wins over a rule that applies to the target
        // status from anywhere.
        if (previousStatus != null) {
            NotificationPolicy exact = byTransition.get(transitionKey(previousStatus, newStatus));
            if (exact != null) {
                return Optional.of(exact);
            }
        }
        return Optional.ofNullable(byTargetStatus.get(newStatus));
    }

    private static String transitionKey(String previousStatus, String newStatus) {
        return previousStatus + "->" + newStatus;
    }
}
