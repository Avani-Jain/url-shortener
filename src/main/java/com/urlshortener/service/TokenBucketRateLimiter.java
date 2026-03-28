package com.urlshortener.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TokenBucketRateLimiter {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.shortener.rate-limit.requests-per-minute:1000}")
    private Long requestsPerMinute;

    @Value("${app.shortener.rate-limit.burst-size:100}")
    private Long burstSize;

    @Value("${app.shortener.rate-limit.enabled:true}")
    private Boolean rateLimitEnabled;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final Long WINDOW_SIZE_SECONDS = 60L;

    public TokenBucketRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String clientId) {
        if (!rateLimitEnabled) {
            return true;
        }

        String key = RATE_LIMIT_PREFIX + clientId;
        long refillRate = requestsPerMinute / 60;

        try {
            String currentTokensStr = redisTemplate.opsForValue().get(key);
            long currentTokens = currentTokensStr != null ? Long.parseLong(currentTokensStr) : burstSize;

            String lastUpdateStr = redisTemplate.opsForValue().get(key + ":last_update");
            long lastUpdate = lastUpdateStr != null ? Long.parseLong(lastUpdateStr) : System.currentTimeMillis();

            long now = System.currentTimeMillis();
            long elapsedSeconds = (now - lastUpdate) / 1000;

            long tokensToAdd = refillRate * elapsedSeconds;
            long newTokenCount = Math.min(currentTokens + tokensToAdd, burstSize);

            if (newTokenCount > 0) {
                newTokenCount--;
                redisTemplate.opsForValue().set(key, String.valueOf(newTokenCount), WINDOW_SIZE_SECONDS * 2, TimeUnit.SECONDS);
                redisTemplate.opsForValue().set(key + ":last_update", String.valueOf(now), WINDOW_SIZE_SECONDS * 2, TimeUnit.SECONDS);

                log.debug("Rate limit - Client: {}, Tokens remaining: {}", clientId, newTokenCount);
                return true;
            } else {
                log.warn("Rate limit exceeded - Client: {}", clientId);
                return false;
            }
        } catch (Exception e) {
            log.error("Error checking rate limit for client: {}", clientId, e);
            return true;
        }
    }

    public long getRemainingTokens(String clientId) {
        String key = RATE_LIMIT_PREFIX + clientId;
        String tokensStr = redisTemplate.opsForValue().get(key);
        return tokensStr != null ? Long.parseLong(tokensStr) : burstSize;
    }

    public void resetRateLimit(String clientId) {
        String key = RATE_LIMIT_PREFIX + clientId;
        redisTemplate.delete(key);
        redisTemplate.delete(key + ":last_update");
        log.info("Rate limit reset for client: {}", clientId);
    }

    public long getRetryAfterSeconds(String clientId) {
        long refillRate = requestsPerMinute / 60;
        return refillRate > 0 ? 60 / refillRate : 60;
    }
}