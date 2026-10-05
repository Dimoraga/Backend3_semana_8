package com.duoc.bbfmobile.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/** Mecanismos de tolerancia a fallos (Resilience4j) para las llamadas a core-banking-api. */
@Slf4j
@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreaker coreCircuitBreaker() {
        CircuitBreaker cb = CircuitBreaker.of("core-banking-api", CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(15))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build());
        cb.getEventPublisher().onStateTransition(e ->
                log.warn("CircuitBreaker core-banking-api: {}", e.getStateTransition()));
        return cb;
    }

    @Bean
    public Retry coreRetry() {
        Retry retry = Retry.of("core-banking-api", RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofMillis(300), 2.0))
                .ignoreExceptions(CallNotPermittedException.class, BulkheadFullException.class)
                .build());
        retry.getEventPublisher().onRetry(e ->
                log.warn("Retry core-banking-api: intento {} fallido", e.getNumberOfRetryAttempts()));
        return retry;
    }

    @Bean
    public Bulkhead coreBulkhead() {
        return Bulkhead.of("core-banking-api", BulkheadConfig.custom()
                .maxConcurrentCalls(10)
                .maxWaitDuration(Duration.ofMillis(200))
                .build());
    }
}
