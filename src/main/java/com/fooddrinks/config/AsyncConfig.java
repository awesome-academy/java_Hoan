package com.fooddrinks.config;

import java.util.concurrent.Executor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;

/**
 * Configures the default {@link Executor} used by all {@code @Async} methods.
 *
 * A bounded thread pool prevents resource exhaustion when orders arrive in
 * bursts — each order triggers at least two async tasks (Slack webhook +
 * email).
 *
 * Pool sizing rationale:
 * {@code corePoolSize=2} — keeps 2 threads alive to handle normal traffic
 * {@code maxPoolSize=10} — caps thread creation under load spikes
 * {@code queueCapacity=100} — buffers up to 100 tasks before rejecting
 */
@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-notif-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> log.error("Uncaught exception in @Async method '{}'", method.getName(), ex);
    }
}
