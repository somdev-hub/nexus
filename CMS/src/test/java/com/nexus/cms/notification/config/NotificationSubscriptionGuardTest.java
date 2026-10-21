package com.nexus.cms.notification.config;

import com.nexus.cms.util.StompPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for the notification subscription guard.
 * <p>
 * The guard is what stops a client reading another organisation's
 * notifications, so its behaviour is worth pinning down: what is allowed,
 * what is refused, and what happens to the connection in each case.
 */
class NotificationSubscriptionGuardTest {

    private final NotificationSubscriptionGuard guard = new NotificationSubscriptionGuard();

    private static final String ORG_TOPIC = "/topic/nexus/notifications/org/123";

    /**
     * Build a SUBSCRIBE frame as the client would send it.
     */
    private Message<?> subscribeFrame(String destination,
                                      Long actingOrgId,
                                      Principal principal,
                                      Map<String, Object> sessionAttributes) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setUser(principal);
        accessor.setSessionAttributes(sessionAttributes);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    /**
     * Session attributes as the CONNECT frame would have left them.
     */
    private Map<String, Object> actingOrgSession(Long orgId) {
        Map<String, Object> attributes = new HashMap<>();
        if (orgId != null) {
            attributes.put(NotificationSubscriptionGuard.ACTING_ORG_SESSION_ATTRIBUTE, orgId);
        }
        return attributes;
    }

    @Test
    void allowsASubscriptionToTheUsersOwnOrganisation() {
        Principal principal = new StompPrincipal("7", "123", List.of(123L, 456L));
        Message<?> message = subscribeFrame(ORG_TOPIC, 123L, principal, actingOrgSession(123L));

        Message<?> result = assertDoesNotThrow(() -> guard.preSend(message, null));

        assertNotNull(result, "the subscription must be allowed through");
        assertSame(message, result);
    }

    @Test
    void refusesASubscriptionToAnotherOrganisation() {
        Principal principal = new StompPrincipal("7", "123", List.of(123L, 456L));
        // The client is acting in 123 but is trying to read organisation 999.
        Message<?> message = subscribeFrame("/topic/nexus/notifications/org/999", 123L, principal,
                actingOrgSession(123L));

        assertThrows(MessagingException.class, () -> guard.preSend(message, null));
    }

    @Test
    void refusesASubscriptionWhenTheUserIsNotAMemberOfTheOrganisation() {
        // The user is a member of 456, not 123.
        Principal principal = new StompPrincipal("7", "456", List.of(456L));
        Message<?> message = subscribeFrame(ORG_TOPIC, 123L, principal, actingOrgSession(123L));

        assertThrows(MessagingException.class, () -> guard.preSend(message, null));
    }

    @Test
    void refusesASubscriptionWhenTheSessionIsAnonymous() {
        Message<?> message = subscribeFrame(ORG_TOPIC, 123L, null, actingOrgSession(123L));

        assertThrows(MessagingException.class, () -> guard.preSend(message, null));
    }

    @Test
    void dropsTheSubscriptionButKeepsTheConnectionWhenTheActingOrganisationIsUnknown() {
        // The client never sent X-Organization-ID. That is a client that has
        // not set the header, not an attempt to reach another organisation's
        // data, so the subscription is dropped rather than the socket closed.
        Principal principal = new StompPrincipal("7", "123", List.of(123L));
        Message<?> message = subscribeFrame(ORG_TOPIC, null, principal, actingOrgSession(null));

        Message<?> result = assertDoesNotThrow(() -> guard.preSend(message, null));

        assertNull(result, "the subscription must be dropped");
    }

    @Test
    void leavesChatDestinationsAlone() {
        Principal principal = new StompPrincipal("7", "123", List.of(123L));
        Message<?> message = subscribeFrame("/topic/conversations/42", 123L, principal, actingOrgSession(123L));

        Message<?> result = assertDoesNotThrow(() -> guard.preSend(message, null));

        assertSame(message, result, "chat subscriptions must be untouched");
    }
}
