package com.nexus.cms.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.core.ConsumerFactory;

/**
 * The listener container factory used by the notification subsystem.
 * <p>
 * The notification listeners get their own container rather than sharing one
 * with the chat and mail listeners. That keeps the two subsystems apart: a
 * notification that fails to be processed cannot affect the chat consumers,
 * and the two can be scaled and tuned independently.
 * <p>
 * The acknowledgement mode is {@code RECORD} rather than the
 * {@code MANUAL_IMMEDIATE} used elsewhere. The notification listener has
 * nothing to acknowledge by hand, and this mode lets the shared error handler
 * commit the offset once a record has been routed to the dead letter topic —
 * without which a poison message would be redelivered forever.
 */
@Configuration
@Slf4j
public class NotificationKafkaConfig {

    @Bean(name = "notificationKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> notificationKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            ObjectProvider<CommonErrorHandler> commonErrorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // One record at a time — a notification is projected and delivered on
        // its own.
        factory.setBatchListener(false);

        // Let the container acknowledge each record, so the error handler can
        // commit the offset after routing a poison record to the dead letter
        // topic.
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        factory.setConcurrency(3);
        factory.getContainerProperties().setPollTimeout(5000);

        // The shared error handler routes a failed record to
        // "<source-topic>-dlq", which for these listeners is
        // core.domain.events-dlq.
        commonErrorHandler.ifAvailable(factory::setCommonErrorHandler);

        log.info("Notification Kafka listener container factory configured — 3 consumers, RECORD acknowledgement");
        return factory;
    }
}
