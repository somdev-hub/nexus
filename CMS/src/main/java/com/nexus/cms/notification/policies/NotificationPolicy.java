package com.nexus.cms.notification.policies;

import java.util.Map;

/**
 * The rule that turns a business event into a notification.
 * <p>
 * A policy answers four questions: is this transition worth telling anybody
 * about, who should be told, how urgent is it, and what should it say.
 * <p>
 * The policy is the <b>presentation</b> half of the system. The producer
 * supplies the facts; the policy decides what they mean to a human. Keeping
 * them apart means the wording can be changed, and the audience rules
 * extended, without touching the producer.
 * <p>
 * The severity, addressing mode and address-to rule are codes rather than
 * Java enums, so that they refer to rows in the notification master tables
 * and can be added or deactivated without a release.
 */
public record NotificationPolicy(
        String severity,
        String titleTemplate,
        String bodyTemplate,
        String addressing,
        String addressTo,
        String requiredAuthority,
        boolean persist
) {

    /**
     * A policy for an event that is worth telling somebody about.
     */
    public static NotificationPolicy of(
            String severity,
            String titleTemplate,
            String bodyTemplate,
            String addressTo) {
        return new NotificationPolicy(
                severity,
                titleTemplate,
                bodyTemplate,
                com.nexus.cms.notification.properties.NotificationMasterCodes.ADDRESSING_ORG,
                addressTo,
                null,
                true);
    }

    /**
     * Render the headline.
     */
    public String renderTitle(Map<String, Object> values) {
        return NotificationTemplateRenderer.render(titleTemplate, values);
    }

    /**
     * Render the body.
     */
    public String renderBody(Map<String, Object> values) {
        return NotificationTemplateRenderer.render(bodyTemplate, values);
    }
}
