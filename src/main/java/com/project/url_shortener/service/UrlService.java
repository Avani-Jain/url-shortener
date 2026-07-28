package com.project.url_shortener.service;

import com.project.url_shortener.entity.UrlMapping;
import com.project.url_shortener.exception.UrlNotFoundException;
import com.project.url_shortener.repository.UrlMappingRepository;
import com.project.url_shortener.util.Base62Encoder;
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
    public String getLongUrl(String shortCode){
        String cacheKey = CACHE_KEY_PREFIX + shortCode;
        String cachedUrl = redisTemplate.opsForValue().get(cacheKey);
        //CACHE-HIT
        if(cachedUrl != null){
            redisTemplate.expire(cacheKey, CACHE_TTL);
            return cachedUrl;
        }
        //CACHE-MISS
        Long id = Base62Encoder.decode(shortCode); //decoding shortcode which is nothing but the ID of database.
        UrlMapping mapping = repository.findById(id)
                .orElseThrow(() -> new UrlNotFoundException("No URL found for code: "+shortCode));
        redisTemplate.opsForValue().set(cacheKey, id.toString(), CACHE_TTL);
        return mapping.getLongUrl();
    }

}
