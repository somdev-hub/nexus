package com.nexus.cms.config;

import com.nexus.cms.util.WebConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka producer configuration.
 * <p>
 * Kept apart from {@link KafkaConfig} so that the dead letter recoverer in
 * {@link KafkaErrorHandlingConfig} can depend on the {@link KafkaTemplate}
 * without creating a circular bean dependency:
 * <pre>
 * KafkaConfig -&gt; CommonErrorHandler -&gt; DeadLetterPublishingRecoverer -&gt; KafkaTemplate
 * KafkaTemplate -&gt; KafkaProducerConfig (this class)
 * </pre>
 * <p>
 * <b>Serialisation</b><br>
 * Keys and values are plain strings. Payloads are serialised to JSON by the
 * producer before publishing, so the broker carries a self-describing document
 * and a consumer can evolve its own model without a coordinated rollout.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerConfig {

    private final WebConstants webConstants;

    /**
     * Producer Factory Configuration
     * Configures high-reliability producer with exactly-once semantics
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Connection
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, webConstants.getBootstrapServers());

        // Serialization
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Durability - Requires all replicas to acknowledge
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");

        // Retry configuration
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 100);

        // Batching and compression for throughput
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        // Timeout configurations
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);

        // Idempotence - Ensures exactly-once semantics without transactions
        // CRITICAL: Do NOT enable transactions (TRANSACTIONAL_ID_CONFIG) as it
        // conflicts with Spring @Transactional
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // Max in-flight requests for idempotent producer
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
