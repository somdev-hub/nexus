package com.nexus.cms.util;

/**
 * STOMP principal naming helpers.
 * <p>
 * The STOMP user destination prefix is {@code /user}. When a message is sent
 * with {@code SimpMessagingTemplate.convertAndSendToUser(name, destination,
 * payload)} the broker resolves it to {@code /user/<name>/<destination>} and
 * delivers it only to sessions whose principal name is exactly
 * {@code <name>}.
 * <p>
 * The principal used by this application is created in
 * {@code com.nexus.cms.chat.config.ChatWebSocketAuthInceptor} and its name is
 * {@code userId + ":" + orgId}. Every call that targets a single user must
 * therefore use the <b>composite</b> name — passing a bare user id silently
 * matches no session and the message is dropped without any error.
 * <p>
 * This class is the single place that knows that format.
 */
public final class StompPrincipalNames {

    /** Separator between the user id and the organisation id in a principal name. */
    public static final char SEPARATOR = ':';

    private StompPrincipalNames() {
    }

    /**
     * Build the STOMP principal name for a user in an organisation.
     *
     * @param userId User id
     * @param orgId  Organisation id, may be null
     * @return the principal name, e.g. {@code 42:7}
     */
    public static String of(Long userId, Long orgId) {
        return userId + String.valueOf(SEPARATOR) + orgId;
    }

    /**
     * Extract the user id from a principal name.
     *
     * @param principalName Principal name, e.g. {@code 42:7}
     * @return the user id, or null if it cannot be parsed
     */
    public static Long userIdOf(String principalName) {
        if (principalName == null || principalName.isBlank()) {
            return null;
        }
        int separator = principalName.indexOf(SEPARATOR);
        String userId = separator > 0 ? principalName.substring(0, separator) : principalName;
        try {
            return Long.valueOf(userId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Extract the organisation id from a principal name.
     *
     * @param principalName Principal name, e.g. {@code 42:7}
     * @return the organisation id, or null if it is absent or not a number
     */
    public static Long orgIdOf(String principalName) {
        if (principalName == null) {
            return null;
        }
        int separator = principalName.indexOf(SEPARATOR);
        if (separator < 0 || separator == principalName.length() - 1) {
            return null;
        }
        try {
            return Long.valueOf(principalName.substring(separator + 1).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
