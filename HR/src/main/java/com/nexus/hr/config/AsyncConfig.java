package com.nexus.hr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous task execution
 * Enables parallel processing of independent operations like PDF generation and DMS uploads
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Thread pool executor for HR document processing tasks
     * Configured for optimal PDF generation and DMS upload operations
     */
    @Bean(name = "hrDocumentTaskExecutor")
    public Executor hrDocumentTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);  // Minimum threads to keep alive
        executor.setMaxPoolSize(10);  // Maximum threads for peak load
        executor.setQueueCapacity(25); // Queue size for pending tasks
        executor.setThreadNamePrefix("hr-doc-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
