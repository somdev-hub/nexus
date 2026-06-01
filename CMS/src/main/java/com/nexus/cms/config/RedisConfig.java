package com.nexus.cms.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.nexus.cms.chat.config.RedisMessageSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Redis Configuration for Chat System
 *
 * Uses Redis for:
 * 1. Session storage - distributes WebSocket sessions across nodes
 * 2. Presence tracking - tracks online users with TTL
 * 3. Temporary data - typing indicators, read receipts cache (optional)
 *
 * Prerequisites:
 * - Redis server running (configured in application.properties)
 * - spring-boot-starter-data-redis dependency
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800) // 30 minutes session timeout
public class RedisConfig {

    /**
     * Configure RedisTemplate for storing objects in Redis
     * Uses JSON serialization for complex objects with proper type handling
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Configure ObjectMapper with type information for deserialization
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL);

        // Use StringRedisSerializer for keys
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // Use Jackson2JsonRedisSerializer for values
        JacksonJsonRedisSerializer<Object> jacksonSerializer = new JacksonJsonRedisSerializer<>(Object.class);

        // Set key-value serializers
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jacksonSerializer);

        // Set hash field-value serializers
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jacksonSerializer);

        // Enable transaction support
        template.setEnableTransactionSupport(true);

        template.afterPropertiesSet();
        return template;
    }

    // for Pub/Sub
    @Bean
    public RedisMessageListenerContainer redisListenerContainer(
            RedisConnectionFactory factory,
            RedisMessageSubscriber subscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(subscriber,
                new PatternTopic("conversation:*")); // listen to all conversation channels
        return container;
    }

    /**
     * Alternative: StringRedisTemplate for simple string operations
     * Useful for presence tracking, counters, cache flags
     * Auto-configured by Spring Boot, available for injection
     */
    // @Bean
    // public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory
    // factory) {
    // return new StringRedisTemplate(factory);
    // }
}
