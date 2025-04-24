package com.notification.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(RedisConnectionFactory.class)
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;
    
    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        Instant start = Instant.now();
        
        try {
            RedisConnection connection = redisConnectionFactory.getConnection();
            String pong = connection.ping();
            long responseTime = Duration.between(start, Instant.now()).toMillis();
            
            details.put("responseTime", responseTime + "ms");
            details.put("version", connection.info().getProperty("redis_version"));
            details.put("mode", connection.info().getProperty("redis_mode"));
            details.put("uptime", connection.info().getProperty("uptime_in_seconds") + "s");
            details.put("connections", connection.info().getProperty("connected_clients"));
            details.put("usedMemory", connection.info().getProperty("used_memory_human"));
            
            connection.close();
            
            if ("PONG".equalsIgnoreCase(pong)) {
                return Health.up().withDetails(details).build();
            } else {
                return Health.down()
                        .withDetail("reason", "Redis did not respond with PONG")
                        .withDetails(details)
                        .build();
            }
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return Health.down()
                    .withDetail("reason", e.getMessage())
                    .withDetail("exception", e.getClass().getName())
                    .build();
        }
    }
} 