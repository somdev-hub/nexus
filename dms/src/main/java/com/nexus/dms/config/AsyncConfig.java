package com.nexus.dms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous task execution
 * Defines the thread pool used for @Async annotated methods
 */
@Configuration
public class AsyncConfig {

    /**
     * Thread pool executor for async tasks
     * Can be customized based on application needs
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core threads that are always running
        executor.setCorePoolSize(5);
        
        // Maximum threads that can be created
        executor.setMaxPoolSize(20);
        
        // Queue capacity for waiting tasks
        executor.setQueueCapacity(500);
        
        // Thread name prefix for debugging
        executor.setThreadNamePrefix("dms-async-");
        
        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // Time to wait for tasks to complete (in seconds)
        executor.setAwaitTerminationSeconds(10);
        
        executor.initialize();
        return executor;
    }
}

