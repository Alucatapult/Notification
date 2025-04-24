package com.notification.metrics;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class NotificationMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter totalNotificationsCounter;
    private final Counter failedNotificationsCounter;
    private final Counter deliveredNotificationsCounter;
    private final Timer notificationProcessingTimer;
    private final Map<String, Counter> notificationTypeCounters;
    private final Map<String, Counter> notificationStatusCounters;
    private final DistributionSummary payloadSizeSummary;

    public NotificationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.notificationTypeCounters = new HashMap<>();
        this.notificationStatusCounters = new HashMap<>();
        
        // Total notifications
        this.totalNotificationsCounter = Counter.builder("notification.count.total")
                .description("Total number of notifications processed")
                .register(meterRegistry);
                
        // Failed notifications
        this.failedNotificationsCounter = Counter.builder("notification.count.failed")
                .description("Number of failed notifications")
                .register(meterRegistry);
                
        // Delivered notifications
        this.deliveredNotificationsCounter = Counter.builder("notification.count.delivered")
                .description("Number of successfully delivered notifications")
                .register(meterRegistry);
                
        // Processing time
        this.notificationProcessingTimer = Timer.builder("notification.processing.time")
                .description("Time taken to process notifications")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
        
        // Payload size
        this.payloadSizeSummary = DistributionSummary.builder("notification.payload.size")
                .description("Distribution of notification payload sizes")
                .baseUnit("bytes")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }
    
    public void recordNotificationProcessed(String type, String status, long processingTimeMs, int payloadSize) {
        // Increment total counter
        totalNotificationsCounter.increment();
        
        // Record processing time
        notificationProcessingTimer.record(processingTimeMs, TimeUnit.MILLISECONDS);
        
        // Record payload size
        payloadSizeSummary.record(payloadSize);
        
        // Increment type-specific counter
        getOrCreateTypeCounter(type).increment();
        
        // Increment status-specific counter
        getOrCreateStatusCounter(status).increment();
        
        // Update delivered/failed specific counters
        if ("DELIVERED".equals(status)) {
            deliveredNotificationsCounter.increment();
        } else if ("FAILED".equals(status)) {
            failedNotificationsCounter.increment();
        }
        
        // Log for debugging
        log.debug("Recorded metrics for notification: type={}, status={}, time={}ms, size={} bytes", 
                type, status, processingTimeMs, payloadSize);
    }
    
    private Counter getOrCreateTypeCounter(String type) {
        return notificationTypeCounters.computeIfAbsent(type, t -> 
            Counter.builder("notification.count.byType")
                .tag("type", t)
                .description("Number of notifications by type")
                .register(meterRegistry)
        );
    }
    
    private Counter getOrCreateStatusCounter(String status) {
        return notificationStatusCounters.computeIfAbsent(status, s -> 
            Counter.builder("notification.count.byStatus")
                .tag("status", s)
                .description("Number of notifications by status")
                .register(meterRegistry)
        );
    }
    
    // Gauge for active notifications in the system
    public void initializeActiveNotificationsGauge(Supplier<Number> activeNotificationsSupplier) {
        Gauge.builder("notification.active", activeNotificationsSupplier)
            .description("Number of active notifications in the system")
            .register(meterRegistry);
    }
} 