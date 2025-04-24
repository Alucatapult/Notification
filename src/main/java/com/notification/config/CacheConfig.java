package com.notification.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Profile("!prod")
    @Primary
    public CacheManager localCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(Arrays.asList("rateLimit", "notifications", "users"));
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .maximumSize(1000));
        return cacheManager;
    }

    @Bean
    @Profile("prod")
    @Primary
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return (builder) -> {
            Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
            
            // Rate limit cache - short TTL
            configMap.put("rateLimit", createCacheConfiguration(Duration.ofMinutes(1)));
            
            // Notifications cache - longer TTL
            configMap.put("notifications", createCacheConfiguration(Duration.ofMinutes(60)));
            
            // Users cache - medium TTL
            configMap.put("users", createCacheConfiguration(Duration.ofMinutes(30)));
            
            builder.withInitialCacheConfigurations(configMap);
        };
    }
    
    private RedisCacheConfiguration createCacheConfiguration(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()));
    }

    // For non-Redis specific caches
    @Bean
    public Caffeine<Object, Object> rateLimitCaffeine() {
        return Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(100);
    }

    @Bean
    public Caffeine<Object, Object> notificationsCaffeine() {
        return Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .maximumSize(500);
    }

    // For specific use cases where local caching is still preferred
    @Bean
    public CaffeineCacheManager rateLimitCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("rateLimit");
        cacheManager.setCaffeine(rateLimitCaffeine());
        return cacheManager;
    }

    @Bean
    public CaffeineCacheManager notificationsCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("notifications");
        cacheManager.setCaffeine(notificationsCaffeine());
        return cacheManager;
    }
} 