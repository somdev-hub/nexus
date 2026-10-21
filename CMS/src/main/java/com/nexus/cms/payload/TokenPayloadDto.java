package com.nexus.cms.payload;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TokenPayloadDto {

    private boolean isValid;
    private List<Map<String, String>> roles;
    private long expiration;
    private long issuedAt;
    private String type;
    private Long userId;
    private Long orgId;
    private String username;
    private String email;

    /**
     * Every organisation the user belongs to.
     * <p>
     * Sourced from the JWT {@code organizations.org_id} claim, which holds a
     * list. {@link #orgId} is the user's <em>home</em> organisation and is not
     * necessarily the one they are currently acting in — the acting
     * organisation travels on the {@code X-Organization-ID} header, exactly as
     * it does on REST calls. Any component that needs to check that a caller
     * may act for a given organisation must validate against this list rather
     * than against {@link #orgId}.
     * <p>
     * May be null or empty when the token carries no such claim; callers must
     * fail closed in that case.
     */
    private List<Long> organizationIds;

    /**
     * The raw {@code organizations} claim as it appears in the token.
     * <p>
     * The claim is a map whose {@code org_id} entry holds the list of
     * organisation ids the user belongs to. It is kept alongside the flattened
     * {@link #organizationIds} so the shape of the claim stays visible.
     */
    private Map<String, Object> organizations;
}
