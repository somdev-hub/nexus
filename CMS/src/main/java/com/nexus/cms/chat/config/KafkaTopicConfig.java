package com.nexus.cms.chat.config;

import com.nexus.cms.chat.properties.ChatConstants;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic messageSentTopic() {
        return TopicBuilder.name(ChatConstants.MESSAGE_SENT_TOPIC)
                .partitions(6)     // partition by conversationId for ordering
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationTopic() {
        return TopicBuilder.name(ChatConstants.NOTIFICATION_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
