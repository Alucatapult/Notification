package com.notification.service;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimitService {
    private final CacheManager cacheManager;
    private static final int MAX_REQUESTS_PER_MINUTE = 100;

    public RateLimitService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public boolean isAllowed(String clientId) {
        Cache cache = cacheManager.getCache("rateLimit");
        AtomicInteger requestCount = cache.get(clientId, AtomicInteger.class);
        
        if (requestCount == null) {
            requestCount = new AtomicInteger(0);
            cache.put(clientId, requestCount);
        }
        
        return requestCount.incrementAndGet() <= MAX_REQUESTS_PER_MINUTE;
    }

    public void resetLimit(String clientId) {
        Cache cache = cacheManager.getCache("rateLimit");
        cache.evict(clientId);
    }
} 