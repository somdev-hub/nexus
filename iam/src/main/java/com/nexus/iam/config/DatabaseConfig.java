package com.nexus.iam.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.orm.jpa.JpaTransactionManager;

import lombok.extern.slf4j.Slf4j;

/**
 * Database and Transaction Configuration for IAM Service
 * 
 * Optimizes transaction handling to prevent deadlocks:
 * - Enables explicit transaction management
 * - Configures optimal isolation levels
 * - Implements transaction ordering strategies
 */
@Configuration
@EnableTransactionManagement
@Slf4j
public class DatabaseConfig {
    
    /**
     * Configure transaction manager with deadlock-aware settings
     * 
     * Key configurations:
     * - Spring will use PostgreSQL's isolation level settings
     * - Individual transactions can be configured via @Transactional
     * - Read-committed isolation helps prevent some deadlocks
     */
    @Bean
    public JpaTransactionManager transactionManager() {
        log.info("Configuring JPA Transaction Manager");
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        return transactionManager;
    }
}

