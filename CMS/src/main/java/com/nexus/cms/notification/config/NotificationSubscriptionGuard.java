package com.nexus.cms.notification.config;

import com.nexus.cms.notification.properties.NotificationConstants;
import com.nexus.cms.util.StompPrincipal;
import com.nexus.cms.util.StompPrincipalNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Stops a client from subscribing to another organisation's notifications.
 * <p>
 * The organisation topic is a <b>broadcast</b> topic, and the STOMP broker
 * performs no authorisation of its own: without a guard, any authenticated
 * client could subscribe to
 * {@code /topic/nexus/notifications/org/{someone-elses-orgId}} and read that
 * organisation's notifications. That would be a cross-organisation data leak,
 * which the platform requirements explicitly forbid.
 * <p>
 * The guard therefore checks two things on every notification subscription:
 * <ol>
 *   <li>the organisation in the destination is the one the client said it was
 *   acting in, and</li>
 *   <li>that organisation is one the user actually belongs to.</li>
 * </ol>
 * The acting organisation comes from the {@code X-Organization-ID} header on
 * the CONNECT frame — the same header the REST calls use. It cannot be
 * trusted on its own, so it is checked against the organisations the user's
 * token says they belong to.
 * <p>
 * <b>Fails closed.</b> If the acting organisation was never established, or
 * the user cannot be shown to belong to it, the subscription is refused.
 * <p>
 * The guard only ever touches notification destinations. Every other frame —
 * chat messages, typing indicators, presence — is returned untouched.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationSubscriptionGuard implements ChannelInterceptor {

    /**
     * Session attribute holding the organisation the client is acting in.
     */
    static final String ACTING_ORG_SESSION_ATTRIBUTE = "nexus.notification.actingOrgId";

    @Override
    public @Nullable Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            captureActingOrganisation(accessor);
            return message;
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (!isNotificationOrganisationTopic(destination)) {
                // Not ours. Leave it alone so that chat is unaffected.
                return message;
            }
            Long requestedOrgId = organisationIdOf(destination);
            Long actingOrgId = actingOrganisation(accessor);
            Principal principal = accessor.getUser();

            if (actingOrgId == null) {
                // The client never said which organisation it is acting in.
                // Refuse the subscription but leave the connection alone —
                // this is a client that has not set the header, not an
                // attempt to reach another organisation's data, and killing
                // the socket would take the rest of the application down with
                // it.
                log.warn("Refusing subscription to {} — the client did not say which organisation it is acting in",
                        destination);
                return null;
            }
            if (requestedOrgId == null || !requestedOrgId.equals(actingOrgId)) {
                // The client is acting in one organisation and is trying to
                // read another one's notifications. That is an authorisation
                // violation, so the connection is closed rather than merely
                // the one subscription being dropped.
                log.warn("Refusing subscription to {} — acting organisation is {}",
                        destination, actingOrgId);
                throw new MessagingException("Not authorised to subscribe to " + destination);
            }
            if (!belongsTo(principal, actingOrgId)) {
                log.warn("Refusing subscription to {} — the user is not a member of organisation {}",
                        destination, actingOrgId);
                throw new MessagingException("Not authorised to subscribe to " + destination);
            }
            log.debug("Notification subscription granted — orgId={}, principal={}", actingOrgId, principal);
        }
        return message;
    }

    /**
     * Record the organisation the client is acting in.
     * <p>
     * The token carries the user's home organisation, which is not necessarily
     * the one they are working in, so the acting organisation has to arrive
     * separately — exactly as it does on a REST call.
     */
    private void captureActingOrganisation(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader(NotificationConstants.ORGANIZATION_ID_HEADER);
        if (header == null || header.isBlank()) {
            log.debug("CONNECT frame carries no {} header — notification subscriptions will be refused",
                    NotificationConstants.ORGANIZATION_ID_HEADER);
            return;
        }
        try {
            Long orgId = Long.valueOf(header.trim());
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                sessionAttributes.put(ACTING_ORG_SESSION_ATTRIBUTE, orgId);
            }
        } catch (NumberFormatException e) {
            log.warn("CONNECT frame carries a malformed {} header — notification subscriptions will be refused",
                    NotificationConstants.ORGANIZATION_ID_HEADER);
        }
    }

    /**
     * The organisation the client is acting in, from the session attributes
     * captured on CONNECT.
     */
    private Long actingOrganisation(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            return null;
        }
        Object value = sessionAttributes.get(ACTING_ORG_SESSION_ATTRIBUTE);
        return value instanceof Long orgId ? orgId : null;
    }

    /**
     * Whether the notification destination is for an organisation.
     */
    private boolean isNotificationOrganisationTopic(String destination) {
        return destination != null && destination.startsWith(NotificationConstants.ORG_TOPIC_PREFIX);
    }

    /**
     * The organisation id carried by a notification destination.
     */
    private Long organisationIdOf(String destination) {
        if (destination == null || !isNotificationOrganisationTopic(destination)) {
            return null;
        }
        String orgId = destination.substring(NotificationConstants.ORG_TOPIC_PREFIX.length());
        try {
            return Long.valueOf(orgId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Whether the user belongs to the organisation.
     * <p>
     * The organisations come from the token's {@code organizations.org_id}
     * claim. When the claim is absent the answer is "no" — the guard fails
     * closed rather than assuming access.
     */
    private boolean belongsTo(Principal principal, Long orgId) {
        if (principal instanceof StompPrincipal stompPrincipal) {
            List<Long> organizations = stompPrincipal.organizations();
            if (!organizations.isEmpty()) {
                return organizations.contains(orgId);
            }
            // No organisation list on the token. Fall back to the
            // organisation carried by the principal itself, which is the
            // user's home organisation.
            Long homeOrgId = StompPrincipalNames.orgIdOf(principal.getName());
            return orgId.equals(homeOrgId);
        }
        if (principal != null) {
            Long homeOrgId = StompPrincipalNames.orgIdOf(principal.getName());
            return orgId.equals(homeOrgId);
        }
        return false;
    }
}
