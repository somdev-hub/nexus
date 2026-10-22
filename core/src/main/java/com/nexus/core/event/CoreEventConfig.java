package com.nexus.core.event;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables scheduling for the outbox relay.
 */
@Configuration
@EnableScheduling
public class CoreEventConfig {
}
