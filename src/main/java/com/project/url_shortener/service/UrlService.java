package com.project.url_shortener.service;

import com.project.url_shortener.entity.UrlMapping;
import com.project.url_shortener.repository.UrlMappingRepository;
import com.project.url_shortener.util.Base62Encoder;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class UrlService {
    private UrlMappingRepository repository;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.base-url}")
    private String baseUrl;

    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final String CACHE_KEY_PREFIX = "url:";

    public UrlService(UrlMappingRepository repository, StringRedisTemplate redisTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
    }

    public String createShortUrl(String longUrl) {
        UrlMapping mapping = new UrlMapping(longUrl);
        UrlMapping saved =  repository.save(mapping);

        String shortCode = Base62Encoder.encode(saved.getId());

        String cacheKey = CACHE_KEY_PREFIX + shortCode;
        redisTemplate.opsForValue().set(cacheKey, longUrl, CACHE_TTL);

        return baseUrl + "/" +shortCode;
    }

}
