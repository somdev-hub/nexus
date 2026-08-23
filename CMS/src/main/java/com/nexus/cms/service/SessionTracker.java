package com.nexus.cms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * SessionTracker manages user presence/session state in Redis
 * 
 * Tracks:
 * - Users online in each conversation
 * - Session persistence across server restarts
 * - Session expiry with TTL
 * 
 * Redis keys:
 * - session:conversation:{conversationId} -> Set of user IDs
 * - session:user:{userId}:conversation:{conversationId} -> session timestamp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionTracker {

    private final RedisTemplate<String, Object> redisTemplate;

    // Session timeout in minutes
    private static final long SESSION_TIMEOUT_MINUTES = 30;

    /**
     * Register user session for a conversation
     * Persists session to Redis with TTL
     * 
     * @param conversationId Conversation ID
     * @param userId         User ID
     */
    public void registerSession(Long conversationId, Long userId) {
        if (conversationId == null || userId == null) {
            log.warn("Cannot register session with null parameters");
            return;
        }

        try {
            String conversationSetKey = "session:conversation:" + conversationId;
            String userSessionKey = "session:user:" + userId + ":conversation:" + conversationId;

            // Add user to conversation's active users set
            redisTemplate.opsForSet().add(conversationSetKey, userId);

            // Set TTL on conversation set
            redisTemplate.expire(conversationSetKey, SESSION_TIMEOUT_MINUTES, TimeUnit.MINUTES);

            // Store user session with TTL
            redisTemplate.opsForValue().set(
                    userSessionKey,
                    System.currentTimeMillis(),
                    SESSION_TIMEOUT_MINUTES,
                    TimeUnit.MINUTES);

            log.debug("Session registered - User: {}, Conversation: {}", userId, conversationId);

        } catch (Exception e) {
            log.error("Error registering session for user {} in conversation {}", userId, conversationId, e);
        }
    }

    /**
     * Unregister user session from conversation
     * Called when user leaves conversation
     * 
     * @param conversationId Conversation ID
     * @param userId         User ID
     */
    public void unregisterSession(Long conversationId, Long userId) {
        if (conversationId == null || userId == null) {
            log.warn("Cannot unregister session with null parameters");
            return;
        }

        try {
            String conversationSetKey = "session:conversation:" + conversationId;
            String userSessionKey = "session:user:" + userId + ":conversation:" + conversationId;

            // Remove user from conversation's active users set
            redisTemplate.opsForSet().remove(conversationSetKey, userId);

            // Delete user session
            Boolean deleted = redisTemplate.delete(userSessionKey);

            log.debug("Session unregistered - User: {}, Conversation: {}", userId, conversationId);

        } catch (Exception e) {
            log.error("Error unregistering session for user {} in conversation {}", userId, conversationId, e);
        }
    }

    /**
     * Get all active users in a conversation
     * Retrieves from Redis - survives across restarts as long as sessions are in
     * TTL
     * 
     * @param conversationId Conversation ID
     * @return Set of active user IDs
     */
    public Set<Long> getActiveUsers(Long conversationId) {
        if (conversationId == null) {
            log.warn("Cannot get active users for null conversation ID");
            return new HashSet<>();
        }

        try {
            String conversationSetKey = "session:conversation:" + conversationId;
            Set<Object> users = redisTemplate.opsForSet().members(conversationSetKey);

            Set<Long> userIds = new HashSet<>();
            if (users != null) {
                users.forEach(user -> {
                    if (user instanceof Number) {
                        userIds.add(((Number) user).longValue());
                    } else if (user instanceof String) {
                        try {
                            userIds.add(Long.parseLong((String) user));
                        } catch (NumberFormatException e) {
                            log.warn("Invalid user ID format in Redis: {}", user);
                        }
                    }
                });
            }

            log.debug("Retrieved {} active users for conversation {}", userIds.size(), conversationId);
            return userIds;

        } catch (Exception e) {
            log.error("Error retrieving active users for conversation {}", conversationId, e);
            return new HashSet<>();
        }
    }

    /**
     * Check if user has active session in conversation
     * 
     * @param conversationId Conversation ID
     * @param userId         User ID
     * @return true if user is online in conversation
     */
    public boolean isUserOnline(Long conversationId, Long userId) {
        if (conversationId == null || userId == null) {
            return false;
        }

        try {
            String conversationSetKey = "session:conversation:" + conversationId;
            Boolean isMember = redisTemplate.opsForSet().isMember(conversationSetKey, userId);
            return isMember != null && isMember;

        } catch (Exception e) {
            log.error("Error checking user online status", e);
            return false;
        }
    }

    /**
     * Refresh session TTL (called on activity like typing, message send, etc.)
     * Keeps session alive across inactivity
     * 
     * @param conversationId Conversation ID
     * @param userId         User ID
     */
    public void refreshSession(Long conversationId, Long userId) {
        if (conversationId == null || userId == null) {
            log.warn("Cannot refresh session with null parameters");
            return;
        }

        try {
            String conversationSetKey = "session:conversation:" + conversationId;
            String userSessionKey = "session:user:" + userId + ":conversation:" + conversationId;

            // Refresh TTL on both keys
            redisTemplate.expire(conversationSetKey, SESSION_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            redisTemplate.expire(userSessionKey, SESSION_TIMEOUT_MINUTES, TimeUnit.MINUTES);

            log.debug("Session refreshed - User: {}, Conversation: {}", userId, conversationId);

        } catch (Exception e) {
            log.error("Error refreshing session", e);
        }
    }

    /**
     * Clear all sessions for a conversation
     * Used during conversation cleanup or testing
     * 
     * @param conversationId Conversation ID
     */
    public void clearConversationSessions(Long conversationId) {
        if (conversationId == null) {
            log.warn("Cannot clear sessions for null conversation ID");
            return;
        }

        try {
            String conversationSetKey = "session:conversation:" + conversationId;
            Set<Object> users = redisTemplate.opsForSet().members(conversationSetKey);

            if (users != null) {
                users.forEach(user -> {
                    if (user instanceof Number) {
                        Long userId = ((Number) user).longValue();
                        unregisterSession(conversationId, userId);
                    }
                });
            }

            log.info("Cleared all sessions for conversation {}", conversationId);

        } catch (Exception e) {
            log.error("Error clearing conversation sessions", e);
        }
    }
}
