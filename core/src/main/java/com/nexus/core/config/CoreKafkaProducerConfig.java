package com.nexus.core.config;

import com.nexus.core.event.OutboxRelayService;
import com.nexus.core.event.outbox.EventOutbox;
import com.nexus.core.event.outbox.EventOutboxRepo;
import com.nexus.core.event.outbox.OutboxStatus;
import com.nexus.core.event.publisher.DomainEventPublisher;
import com.nexus.core.event.publisher.OutboxDomainEventPublisher;
import com.nexus.core.event.transport.EventTransport;
import com.nexus.core.event.transport.KafkaEventTransport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Kafka producer configuration for the event subsystem.
 * <p>
 * Core is a producer only: it publishes domain events to Kafka for the CMS
 * consumer to project. It has no listeners and no broker configuration of
 * its own beyond the producer.
 */
@Configuration
public class CoreKafkaProducerConfig {

    @Bean
    public org.springframework.transaction.support.TransactionTemplate transactionTemplate(
            org.springframework.transaction.PlatformTransactionManager transactionManager) {
        return new org.springframework.transaction.support.TransactionTemplate(transactionManager);
    }

    @Bean
    public ProducerFactory<String, Object> coreProducerFactory(org.springframework.core.env.Environment env) {
        org.apache.kafka.clients.producer.ProducerConfig cfg = null;
        java.util.Map<String, Object> configProps = new java.util.HashMap<>();
        configProps.put(org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                env.getProperty("spring.kafka.bootstrap-servers", "localhost:9092"));
        configProps.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringSerializer.class);
        configProps.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringSerializer.class);
        configProps.put(org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(org.apache.kafka.clients.producer.ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(org.apache.kafka.clients.producer.ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 100);
        configProps.put(org.apache.kafka.clients.producer.ProducerConfig.LINGER_MS_CONFIG, 10);
        configProps.put(org.apache.kafka.clients.producer.ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(org.apache.kafka.clients.producer.ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        configProps.put(org.apache.kafka.clients.producer.ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        configProps.put(org.apache.kafka.clients.producer.ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(org.apache.kafka.clients.producer.ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
        configProps.put(org.apache.kafka.clients.producer.ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(org.apache.kafka.clients.producer.ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> coreProducerFactory) {
        return new KafkaTemplate<>(coreProducerFactory);
    }

    @Bean
    public EventTransport eventTransport(KafkaTemplate<String, Object> kafkaTemplate,
                                        com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new KafkaEventTransport(kafkaTemplate, objectMapper);
    }

    /**
     * The outbox relay. Runs on a fixed delay, publishes the pending rows in
     * batches, re-arms the failed ones, and purges the expired ones.
     */
    @Bean
    public OutboxRelayService outboxRelayService(EventOutboxRepo outboxRepo,
                                                 EventTransport eventTransport,
                                                 org.springframework.transaction.support.TransactionTemplate transactionTemplate,
                                                 @org.springframework.beans.factory.annotation.Value("${outbox.relay.enabled:true}") boolean relayEnabled,
                                                 @org.springframework.beans.factory.annotation.Value("${outbox.relay.max-attempts:10}") int maxAttempts,
                                                 @org.springframework.beans.factory.annotation.Value("${outbox.relay.batch-size:100}") int batchSize,
                                                 @org.springframework.beans.factory.annotation.Value("${outbox.failed-retention.days:30}") int failedRetentionDays,
                                                 @org.springframework.beans.factory.annotation.Value("${outbox.replay.enabled:true}") boolean replayEnabled,
                                                 @org.springframework.beans.factory.annotation.Value("${outbox.replay.max-replays:3}") int maxReplays,
                                                 @org.springframework.beans.factory.annotation.Value("${outbox.replay.cooldown-seconds:60}") long cooldownSeconds) {
        return new OutboxRelayService(outboxRepo, eventTransport, transactionTemplate, relayEnabled, maxAttempts, batchSize, failedRetentionDays,
                replayEnabled, maxReplays, cooldownSeconds);
    }
}
