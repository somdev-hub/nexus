package com.nexus.nexusbuddy.util;

/**
 * ThreadLocal holder for request context (orgId, userId) during chat processing.
 * Set by ChatServiceImpl before AI invocation, read by DynamicToolProvider during tool execution.
 */
public class RequestContext {
    private static final ThreadLocal<Long> ORG_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();

    public static void setOrgId(Long orgId) {
        ORG_ID.set(orgId);
    }

    public static Long getOrgId() {
        return ORG_ID.get();
    }

    public static void setUserId(String userId) {
        USER_ID.set(userId);
    }

    public static String getUserId() {
        return USER_ID.get();
    }

    public static void clear() {
        ORG_ID.remove();
        USER_ID.remove();
    }
}