package com.nexus.cms.config;

import com.nexus.cms.util.WebConstants;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Production-Ready Kafka Configuration
 *
 * This configuration provides:
 * - High availability and fault tolerance
 * - Exactly-once semantics
 * - Error handling and retry logic
 * - Performance optimization
 * - Monitoring capabilities
 */
@Configuration
@EnableKafka
@RequiredArgsConstructor
public class KafkaConfig {

    private final WebConstants webConstants;

    /**
     * Kafka Admin Configuration
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, webConstants.getBootstrapServers());
        return new KafkaAdmin(configs);
    }

    /**
     * Producer Factory Configuration
     * Configures high-reliability producer with exactly-once semantics
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
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

        // Idempotence - Ensures exactly-once semantics
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // Transactional configuration
        configProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "cms-producer-");

        // Max in-flight requests (must be 1 for ordering guarantee with idempotence)
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Consumer Factory Configuration
     * Configures reliable consumer with manual commit and batch processing
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Connection
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, webConstants.getBootstrapServers());
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, webConstants.getConsumerGroupId());

        // Deserialization - Use StringDeserializer for both key and value
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Offset Management - Start from earliest unread message
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Auto-commit configuration
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        configProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 100);

        // Session timeout configuration
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);

        // Fetch configuration for performance
        configProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);
        configProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 5000);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);

        // Isolation level for reading only committed messages
        configProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * Kafka Listener Container Factory for Batch Processing
     */
    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // Enable batch processing
        factory.setBatchListener(true);

        // Manual acknowledgment for better control
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Concurrency level (number of threads)
        factory.setConcurrency(5);

        // Poll timeout
        factory.getContainerProperties().setPollTimeout(5000);

        // Monitor interval
        factory.getContainerProperties().setMonitorInterval(30);

        return factory;
    }

    /**
     * Alternative Listener Container Factory for Single Record Processing
     */
    @Bean(name = "singleRecordKafkaListenerContainerFactory")
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> singleRecordKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // Single record processing (default)
        factory.setBatchListener(false);

        // Manual acknowledgment
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Concurrency level
        factory.setConcurrency(3);

        // Poll timeout
        factory.getContainerProperties().setPollTimeout(5000);

        return factory;
    }

    /**
     * Define topics programmatically
     * For production, you may want to manage topics externally
     */
    @Bean
    public Object notificationTopic() {
        return TopicBuilder.name("cms-notifications")
            .partitions(3)
            .replicas(1)
            .config("retention.ms", "604800000") // 7 days
            .config("compression.type", "snappy")
            .config("segment.ms", "86400000") // 1 day
            .build();
    }

    @Bean
    public Object auditTopic() {
        return TopicBuilder.name("cms-audit-logs")
            .partitions(3)
            .replicas(1)
            .config("retention.ms", "2592000000") // 30 days
            .config("compression.type", "snappy")
            .build();
    }

    @Bean
    public Object eventTopic() {
        return TopicBuilder.name("cms-events")
            .partitions(5)
            .replicas(1)
            .config("retention.ms", "1209600000") // 14 days
            .config("compression.type", "snappy")
            .build();
    }

    @Bean
    public Object candidateSelectionMailTopic(){
        return TopicBuilder.name("candidate-selection-mail")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "1209600000")
                .config("compression.type", "snappy")
                .build();
    }

    @Bean
    public Object candidateRejectionMailTopic(){
        return TopicBuilder.name("candidate-rejection-mail")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "1209600000")
                .config("compression.type", "snappy")
                .build();
    }

    @Bean
    public Object candidatePromotionMailTopic(){
        return TopicBuilder.name("candidate-promotion-mail")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "1209600000")
                .config("compression.type", "snappy")
                .build();
    }
}





