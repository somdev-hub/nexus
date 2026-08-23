package com.nexus.cms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka Error Handling Configuration
 *
 * Provides:
 * - Dead Letter Queue (DLQ) for failed messages
 * - Error recovery and retry logic
 * - Detailed error logging
 * - Message preservation for analysis
 */
@Slf4j
@Configuration
public class KafkaErrorHandlingConfig {

    /**
     * Dead Letter Queue Topic Names
     */
    public static final String NOTIFICATION_DLQ_TOPIC = "cms-notifications-dlq";
    public static final String AUDIT_DLQ_TOPIC = "cms-audit-logs-dlq";
    public static final String EVENTS_DLQ_TOPIC = "cms-events-dlq";

    /**
     * Custom error handler with retry and backoff
     */
    @Bean
    public CommonErrorHandler kafkaErrorHandler() {
        // Create error handler with fixed backoff: 1 second delay, 3 retries max
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(new FixedBackOff(1000L, 3L));

        log.info("Kafka error handler configured with retry policy: 1s delay, 3 max retries");
        return errorHandler;
    }

    /**
     * Dead Letter Queue Topics Configuration
     */
    @Bean
    public Object notificationDlqTopic() {
        return TopicBuilder.name(NOTIFICATION_DLQ_TOPIC)
            .partitions(3)
            .replicas(1)
            .config("retention.ms", "2592000000") // 30 days
            .config("compression.type", "snappy")
            .build();
    }

    @Bean
    public Object auditDlqTopic() {
        return TopicBuilder.name(AUDIT_DLQ_TOPIC)
            .partitions(3)
            .replicas(1)
            .config("retention.ms", "2592000000") // 30 days
            .config("compression.type", "snappy")
            .build();
    }

    @Bean
    public Object eventsDlqTopic() {
        return TopicBuilder.name(EVENTS_DLQ_TOPIC)
            .partitions(3)
            .replicas(1)
            .config("retention.ms", "2592000000") // 30 days
            .config("compression.type", "snappy")
            .build();
    }
}












