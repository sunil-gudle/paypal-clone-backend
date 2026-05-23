package com.dailyCode.paypal_user.common.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    private final StringRedisTemplate redisTemplate;

    @Value("${app.rate-limit.capacity}")
    private int capacity;

    @Value("${app.rate-limit.refill-tokens}")
    private int refillTokens;

    @Value("${app.rate-limit.refill-seconds}")
    private long refillSeconds;

    /**
     * Returns true if the request is allowed, false if rate limit exceeded.
     * Uses a simple sliding window counter in Redis.
     */
    public boolean isAllowed(String key) {
        String redisKey = RATE_LIMIT_PREFIX + key;

        Long current = redisTemplate.opsForValue().increment(redisKey);

        if (current == null) return false;

        // First request — set TTL window
        if (current == 1) {
            redisTemplate.expire(redisKey, Duration.ofSeconds(refillSeconds));
        }

        if (current > capacity) {
            log.warn("Rate limit exceeded for key: {}", key);
            return false;
        }

        return true;
    }

    public long getRemaining(String key) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        String value = redisTemplate.opsForValue().get(redisKey);
        if (value == null) return capacity;
        return Math.max(0, capacity - Long.parseLong(value));
    }

    public long getTtl(String key) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        Long ttl = redisTemplate.getExpire(redisKey);
        return ttl != null ? ttl : 0;
    }
}
