package com.urlshortener.service;

import com.urlshortener.domain.ShortenedUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.shortener.cache-ttl-hours:24}")
    private Long cacheTtlHours;

    private static final String CACHE_PREFIX = "url:";
    private static final String SHORT_CODE_PREFIX = "short:";

    public CacheService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<ShortenedUrl> getFromCache(String shortCode) {
        try {
            String key = CACHE_PREFIX + shortCode;
            String cached = redisTemplate.opsForValue().get(key);

            if (cached != null) {
                ShortenedUrl url = objectMapper.readValue(cached, ShortenedUrl.class);
                log.debug("Cache HIT for short code: {}", shortCode);
                return Optional.of(url);
            }
            log.debug("Cache MISS for short code: {}", shortCode);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error reading from cache for short code: {}", shortCode, e);
            return Optional.empty();
        }
    }

    public void putInCache(ShortenedUrl url) {
        try {
            String key = CACHE_PREFIX + url.getShortCode();
            String value = objectMapper.writeValueAsString(url);
            redisTemplate.opsForValue().set(key, value, cacheTtlHours, TimeUnit.HOURS);
            log.debug("Cached URL with short code: {}", url.getShortCode());
        } catch (Exception e) {
            log.error("Error writing to cache for short code: {}", url.getShortCode(), e);
        }
    }

    public void updateInCache(ShortenedUrl url) {
        try {
            String key = CACHE_PREFIX + url.getShortCode();
            String value = objectMapper.writeValueAsString(url);
            redisTemplate.opsForValue().set(key, value, cacheTtlHours, TimeUnit.HOURS);
            log.debug("Updated cached URL with short code: {}", url.getShortCode());
        } catch (Exception e) {
            log.error("Error updating cache for short code: {}", url.getShortCode(), e);
        }
    }

    public void invalidateCache(String shortCode) {
        try {
            String key = CACHE_PREFIX + shortCode;
            Boolean deleted = redisTemplate.delete(key);
            if (deleted != null && deleted) {
                log.debug("Invalidated cache for short code: {}", shortCode);
            }
        } catch (Exception e) {
            log.error("Error invalidating cache for short code: {}", shortCode, e);
        }
    }

    public void cacheOriginalUrlMapping(String originalUrl, String shortCode) {
        try {
            String key = SHORT_CODE_PREFIX + originalUrl.hashCode();
            redisTemplate.opsForValue().set(key, shortCode, cacheTtlHours, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("Error caching URL mapping", e);
        }
    }

    public Optional<String> getCachedShortCode(String originalUrl) {
        try {
            String key = SHORT_CODE_PREFIX + originalUrl.hashCode();
            String shortCode = redisTemplate.opsForValue().get(key);
            return shortCode != null ? Optional.of(shortCode) : Optional.empty();
        } catch (Exception e) {
            log.error("Error retrieving cached short code", e);
            return Optional.empty();
        }
    }

    public Long getCacheSize() {
        try {
            return (long) redisTemplate.keys(CACHE_PREFIX + "*").size();
        } catch (Exception e) {
            log.error("Error getting cache size", e);
            return 0L;
        }
    }

    public void clearCache() {
        try {
            redisTemplate.getConnectionFactory().getConnection().flushDb();
            log.info("Cache flushed");
        } catch (Exception e) {
            log.error("Error flushing cache", e);
        }
    }
}