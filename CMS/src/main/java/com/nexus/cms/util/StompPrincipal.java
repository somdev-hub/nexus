package com.nexus.cms.util;

import java.security.Principal;
import java.util.List;

/**
 * The STOMP principal of a connected user.
 * <p>
 * It lives in the shared util package rather than in the chat package so that
 * the chat and notification subsystems can both depend on it without
 * depending on each other.
 * <p>
 * The name is the composite {@code userId:orgId} — see
 * {@link StompPrincipalNames}. The organisation list is the set of
 * organisations the user belongs to, taken from the token's
 * {@code organizations.org_id} claim. The <em>acting</em> organisation is
 * whichever one the user is currently working in; it travels on the
 * {@code X-Organization-ID} header and is not necessarily the same as the
 * organisation in the token.
 */
public record StompPrincipal(String userId, String orgId, List<Long> organizationIds) implements Principal {

    @Override
    public String getName() {
        return userId + StompPrincipalNames.SEPARATOR + orgId;
    }

    /**
     * The organisations the user belongs to, never null.
     */
    public List<Long> organizations() {
        return organizationIds != null ? organizationIds : List.of();
    }
}
