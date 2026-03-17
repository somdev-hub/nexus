package com.nexus.cms.util;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class WebConstants {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroupId;

    @Value("${spring.mail.from:noreply@nexushr.com}")
    private String defaultFromEmail;

    @Value("${spring.mail.max-recipients:100}")
    private Integer maxRecipients;
}
