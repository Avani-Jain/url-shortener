package com.project.url_shortener.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;

@Service
public class RateLimiterService {
    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> rateLimiterScript;
    @Value("${ratelimit.capacity}")
    private long capacity;
    @Value("${ratelimit.refill-rate}")
    private long refillRate;

    public RateLimiterService(StringRedisTemplate redisTemplate, RedisScript<Long> rateLimiterScript) {
        this.redisTemplate = redisTemplate;
        this.rateLimiterScript = rateLimiterScript;
    }
    public boolean isAllowed(String clientIp){
        String key = "ratelimit:" + clientIp;
        System.out.println("====================================================KEY: " + key);
        long now = Instant.now().getEpochSecond();
        Long result = redisTemplate.execute(rateLimiterScript, Collections.singletonList(key),
                String.valueOf(capacity),
                String.valueOf(refillRate),
                String.valueOf(now));
        return result!=null && result ==1L;
    }
}
