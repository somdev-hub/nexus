package com.nexus.iam.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Centralized Caffeine Cache Configuration
 * Configures local in-memory caching using Caffeine for improved application performance
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

    /**
     * Cache names used throughout the application
     */
    public static final String ORGANIZATION_CACHE = "organizations";
    public static final String EMPLOYEE_DETAILS_CACHE = "employeeDetails";
    public static final String EMPLOYEE_DIRECTORY_CACHE = "employeeDirectory";
    public static final String EMPLOYEE_INSIGHTS_CACHE = "employeeInsights";
    public static final String USER_ORGANIZATION_DETAILS_CACHE = "userOrganizationDetails";

    /**
     * Configures the Caffeine Cache Manager with default and specific cache configurations
     *
     * @return CacheManager configured with Caffeine
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                ORGANIZATION_CACHE,
                EMPLOYEE_DETAILS_CACHE,
                EMPLOYEE_DIRECTORY_CACHE,
                EMPLOYEE_INSIGHTS_CACHE,
                USER_ORGANIZATION_DETAILS_CACHE
        );

        // Set default cache configuration
        cacheManager.setCaffeine(Caffeine.newBuilder()
                // Maximum size of cache entries
                .maximumSize(1000)
                // Expire entries after they have been accessed
                .expireAfterAccess(10, TimeUnit.MINUTES)
                // Enable statistics for monitoring
                .recordStats()
        );

        log.info("Caffeine Cache Manager initialized with {} cache names", 5);
        return cacheManager;
    }

    /**
     * Gets default cache configuration builder
     * Used for creating consistent cache behavior across the application
     *
     * @return Caffeine builder with default settings
     */
    public static Caffeine<Object, Object> getDefaultCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .recordStats();
    }

    /**
     * Gets cache configuration for employee details (shorter TTL due to frequent updates)
     *
     * @return Caffeine builder configured for employee details
     */
    public static Caffeine<Object, Object> getEmployeeDetailsCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats();
    }

    /**
     * Gets cache configuration for employee directory (medium TTL)
     *
     * @return Caffeine builder configured for employee directory
     */
    public static Caffeine<Object, Object> getEmployeeDirectoryCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterAccess(8, TimeUnit.MINUTES)
                .recordStats();
    }

    /**
     * Gets cache configuration for employee insights (longer TTL for aggregated data)
     *
     * @return Caffeine builder configured for employee insights
     */
    public static Caffeine<Object, Object> getEmployeeInsightsCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .recordStats();
    }
}

