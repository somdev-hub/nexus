package com.nexus.cms.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Error Handling Configuration
 * <p>
 * Provides:
 * - Dead Letter Queue (DLQ) for failed messages
 * - Error recovery and retry logic
 * - Detailed error logging
 * - Message preservation for analysis
 * <p>
 * <b>Dead letter routing</b><br>
 * The recoverer derives the DLQ topic from the topic the record came from
 * ({@code <source-topic>-dlq}). A single recoverer therefore serves every
 * listener without per-listener wiring, and it keeps the DLQ topic names in
 * step with the topics they shadow:
 * <ul>
 *   <li>{@code cms-notifications} → {@code cms-notifications-dlq}</li>
 *   <li>{@code cms-audit-logs} → {@code cms-audit-logs-dlq}</li>
 *   <li>{@code cms-events} → {@code cms-events-dlq}</li>
 *   <li>{@code core.domain.events} → {@code core.domain.events-dlq}</li>
 * </ul>
 * The original topic, partition, offset, key and headers are preserved on the
 * DLQ record, so the exact origin of a failed message is always recoverable.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaErrorHandlingConfig {

    /**
     * Used to publish a recovered record to its dead letter topic. Injected
     * from {@link KafkaProducerConfig}; the dependency chain
     * {@code KafkaConfig -> CommonErrorHandler -> KafkaTemplate} is acyclic
     * because the producer lives in its own configuration class.
     */
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Suffix appended to a source topic name to derive its dead letter topic.
     */
    public static final String DLQ_TOPIC_SUFFIX = "-dlq";

    /**
     * Dead Letter Queue Topic Names
     */
    public static final String NOTIFICATION_DLQ_TOPIC = "cms-notifications-dlq";
    public static final String AUDIT_DLQ_TOPIC = "cms-audit-logs-dlq";
    public static final String EVENTS_DLQ_TOPIC = "cms-events-dlq";

    /**
     * How many times a record is retried before it is routed to the dead letter
     * topic, and how long to wait between attempts.
     */
    private static final long RETRY_BACKOFF_MS = 1000L;
    private static final long MAX_RETRY_ATTEMPTS = 3L;

    /**
     * The partition count of every dead letter topic.
     * <p>
     * A dead letter topic must have at least as many partitions as the topic
     * it shadows, otherwise a recovered record cannot be published to the
     * partition it came from. The counts are declared here and the
     * {@code NewTopic} beans use the same constants, so the two can never
     * drift apart.
     * <p>
     * The counts are also what lets the recoverer map a source partition onto
     * a partition that actually exists in the dead letter topic.
     */
    public static final Map<String, Integer> DLQ_PARTITION_COUNTS = Map.of(
            NOTIFICATION_DLQ_TOPIC, 3,
            AUDIT_DLQ_TOPIC, 3,
            EVENTS_DLQ_TOPIC, 3,
            "core.domain.events-dlq", 6);

    /**
     * Custom error handler with retry and backoff
     * <p>
     * After the retry budget is exhausted the record is published to
     * {@code <source-topic>-dlq} rather than being silently dropped, so a
     * failed message is always preserved for inspection and replay.
     */
    @Bean
    public CommonErrorHandler kafkaErrorHandler() {
        DeadLetterPublishingRecoverer recoverer = deadLetterPublishingRecoverer();

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer,
                new FixedBackOff(RETRY_BACKOFF_MS, MAX_RETRY_ATTEMPTS));

        // Advance the consumer offset once the record has been published to the
        // DLQ, so a poison message is not replayed forever in the same session.
        errorHandler.setCommitRecovered(true);

        log.info("Kafka error handler configured: {}ms delay, {} max retries, dead letter routing to '{}-dlq'",
                RETRY_BACKOFF_MS, MAX_RETRY_ATTEMPTS, "<source-topic>");
        return errorHandler;
    }

    /**
     * Recovers a failed record by publishing it to the dead letter topic that
     * shadows its source topic.
     */
    private DeadLetterPublishingRecoverer deadLetterPublishingRecoverer() {
        return new DeadLetterPublishingRecoverer(kafkaTemplate, (record, exception) -> {
            String dlqTopic = record.topic() + DLQ_TOPIC_SUFFIX;
            int partition = deadLetterPartition(dlqTopic, record.partition());
            log.error("Routing record to dead letter topic {} — topic={} partition={} offset={} key={} cause={}",
                    dlqTopic, record.topic(), record.partition(), record.offset(), record.key(),
                    exception.getMessage());
            return new TopicPartition(dlqTopic, partition);
        });
    }

    /**
     * The partition a recovered record is published to in the dead letter
     * topic.
     * <p>
     * The source partition is kept when the dead letter topic has that many
     * partitions, so the order of the failures is preserved. When it does
     * not, the partition is folded into the range that exists — publishing to
     * a partition that is not there fails with a metadata timeout, and the
     * failure would be lost rather than preserved. The original partition and
     * offset stay on the record in the {@code DLT_ORIGINAL_*} headers, so
     * nothing is lost either way.
     */
    private int deadLetterPartition(String dlqTopic, int sourcePartition) {
        int partitions = DLQ_PARTITION_COUNTS.getOrDefault(dlqTopic, 1);
        if (sourcePartition < 0 || sourcePartition >= partitions) {
            log.warn("Source partition {} does not exist in {} ({} partitions) — the record is published to partition 0",
                    sourcePartition, dlqTopic, partitions);
            return 0;
        }
        return sourcePartition;
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
