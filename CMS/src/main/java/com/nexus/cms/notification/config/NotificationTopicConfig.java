package com.nexus.cms.notification.config;

import com.nexus.cms.notification.properties.NotificationConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * The Kafka topics used by the notification subsystem.
 * <p>
 * The dead letter topic is created here rather than relying on the shared
 * error handler to derive it, so that the topic exists with the right
 * partition count and retention whatever the broker is configured to do.
 */
@Configuration
@Slf4j
public class NotificationTopicConfig {

    /**
     * The number of partitions on the events topic and its dead letter topic.
     * <p>
     * They have to match, so that a record recovered from the events topic
     * can be published to the same partition in the dead letter topic. The
     * count is also declared in {@code KafkaErrorHandlingConfig}, which uses
     * it to map a source partition onto a partition that exists.
     */
    public static final int EVENTS_PARTITIONS = 6;

    /**
     * The topic carrying domain events published by {@code nexus/core}.
     * <p>
     * Six partitions: the message key is the target organisation id, so
     * events for one organisation always land on the same partition and keep
     * their order.
     */
    @Bean
    public NewTopic notificationEventsTopic() {
        log.info("Creating topic: {}", NotificationConstants.EVENTS_TOPIC);
        return TopicBuilder.name(NotificationConstants.EVENTS_TOPIC)
                .partitions(EVENTS_PARTITIONS)
                .replicas(1)
                .config("retention.ms", "1209600000") // 14 days
                .config("compression.type", "snappy")
                .build();
    }

    /**
     * The dead letter topic for {@link #notificationEventsTopic()}.
     * <p>
     * It has the same number of partitions as the topic it shadows, so a
     * recovered record can be published to the partition it came from.
     */
    @Bean
    public NewTopic notificationEventsDlqTopic() {
        log.info("Creating topic: {}", NotificationConstants.EVENTS_DLQ_TOPIC);
        return TopicBuilder.name(NotificationConstants.EVENTS_DLQ_TOPIC)
                .partitions(EVENTS_PARTITIONS)
                .replicas(1)
                .config("retention.ms", "2592000000") // 30 days
                .config("compression.type", "snappy")
                .build();
    }
}
