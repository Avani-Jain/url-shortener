package com.urlshortener.service;

import com.urlshortener.domain.ShortenedUrl;
import com.urlshortener.dto.*;
import com.urlshortener.repository.ShortenedUrlRepository;
import com.urlshortener.util.Base62Encoder;

// Import exception classes explicitly (NOT wildcard)
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.exception.InvalidUrlException;
import com.urlshortener.exception.DuplicateShortCodeException;
import com.urlshortener.exception.RateLimitExceededException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class UrlShorteningService {

    private final ShortenedUrlRepository repository;
    private final CacheService cacheService;
    private final TokenBucketRateLimiter rateLimiter;

    @Value("${app.shortener.base-url:http://localhost:8080/api}")
    private String baseUrl;

    private static volatile long idCounter = 0;

    @Autowired
    public UrlShorteningService(
        ShortenedUrlRepository repository,
        CacheService cacheService,
        TokenBucketRateLimiter rateLimiter
    ) {
        this.repository = repository;
        this.cacheService = cacheService;
        this.rateLimiter = rateLimiter;
        initializeCounter();
    }

    @Transactional
    public ShortenUrlResponse shortenUrl(ShortenUrlRequest request, String clientIp) {
        if (!rateLimiter.isAllowed(clientIp)) {
            throw new RateLimitExceededException(
                "Rate limit exceeded. Max 1000 requests per minute.",
                rateLimiter.getRetryAfterSeconds(clientIp)
            );
        }

        if (request.getOriginalUrl() == null || request.getOriginalUrl().isBlank()) {
            throw new InvalidUrlException("Original URL cannot be empty");
        }

        String originalUrl = request.getOriginalUrl().trim();

        Optional<ShortenedUrl> existingUrl = repository.findByOriginalUrl(originalUrl);
        if (existingUrl.isPresent()) {
            log.info("URL already shortened: {}", originalUrl);
            return buildResponse(existingUrl.get());
        }

        String shortCode;
        if (request.getCustomShortCode() != null && !request.getCustomShortCode().isBlank()) {
            shortCode = validateAndUseCustomCode(request.getCustomShortCode());
        } else {
            shortCode = generateUniqueShortCode();
        }

        ShortenedUrl shortenedUrl = ShortenedUrl.builder()
            .shortCode(shortCode)
            .originalUrl(originalUrl)
            .expiresAt(request.getExpiresAt())
            .clickCount(0L)
            .isActive(true)
            .creatorIp(clientIp)
            .build();

        ShortenedUrl saved = repository.save(shortenedUrl);
        log.info("URL shortened successfully - Short code: {}, Original: {}", shortCode, originalUrl);

        cacheService.putInCache(saved);
        cacheService.cacheOriginalUrlMapping(originalUrl, shortCode);

        return buildResponse(saved);
    }

    @Transactional
    public String resolveUrl(String shortCode) {
        Optional<ShortenedUrl> cachedUrl = cacheService.getFromCache(shortCode);

        ShortenedUrl url;
        if (cachedUrl.isPresent()) {
            url = cachedUrl.get();
        } else {
            // ✅ FIXED: Changed from new Exceptions(...) to new ResourceNotFoundException(...)
            url = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short code not found: " + shortCode));
            cacheService.putInCache(url);
        }

        // ✅ FIXED: Changed from new Exceptions(...) to new ResourceNotFoundException(...)
        if (!url.isValid()) {
            throw new ResourceNotFoundException("URL has expired or is inactive");
        }

        incrementClickCountAsync(url);

        log.info("URL resolved - Short code: {}, Redirecting to: {}", shortCode, url.getOriginalUrl());
        return url.getOriginalUrl();
    }

    public AnalyticsResponse getAnalytics(String shortCode) {
        // ✅ FIXED: Changed from new Exceptions(...) to new ResourceNotFoundException(...)
        ShortenedUrl url = repository.findByShortCode(shortCode)
            .orElseThrow(() -> new ResourceNotFoundException("Short code not found: " + shortCode));

        LocalDateTime createdAt = url.getCreatedAt();
        LocalDateTime now = LocalDateTime.now();
        long daysSinceCreation = java.time.temporal.ChronoUnit.DAYS.between(createdAt, now) + 1;

        Double avgClicksPerDay = daysSinceCreation > 0 
            ? (double) url.getClickCount() / daysSinceCreation 
            : url.getClickCount().doubleValue();

        return AnalyticsResponse.builder()
            .shortCode(shortCode)
            .originalUrl(url.getOriginalUrl())
            .totalClicks(url.getClickCount())
            .clicksToday(0L)
            .clicksThisWeek(0L)
            .clicksThisMonth(0L)
            .createdAt(createdAt)
            .avgClicksPerDay(avgClicksPerDay)
            .build();
    }

    public UrlDetailsResponse getUrlDetails(String shortCode) {
        // ✅ FIXED: Changed from new Exceptions(...) to new ResourceNotFoundException(...)
        ShortenedUrl url = repository.findByShortCode(shortCode)
            .orElseThrow(() -> new ResourceNotFoundException("Short code not found: " + shortCode));

        return UrlDetailsResponse.builder()
            .shortCode(url.getShortCode())
            .originalUrl(url.getOriginalUrl())
            .createdAt(url.getCreatedAt())
            .updatedAt(url.getUpdatedAt())
            .expiresAt(url.getExpiresAt())
            .clickCount(url.getClickCount())
            .isActive(url.getIsActive())
            .creatorIp(url.getCreatorIp())
            .build();
    }

    @Transactional
    public void deleteUrl(String shortCode) {
        // ✅ FIXED: Changed from new Exceptions(...) to new ResourceNotFoundException(...)
        ShortenedUrl url = repository.findByShortCode(shortCode)
            .orElseThrow(() -> new ResourceNotFoundException("Short code not found: " + shortCode));

        url.setIsActive(false);
        repository.save(url);
        cacheService.invalidateCache(shortCode);

        log.info("URL marked as inactive - Short code: {}", shortCode);
    }

    private String generateUniqueShortCode() {
        String shortCode;
        int attempts = 0;
        int maxAttempts = 5;

        do {
            long counter = getNextCounter();
            shortCode = Base62Encoder.generateShortCodeWithCounter(counter);

            if (attempts++ > maxAttempts) {
                shortCode = Base62Encoder.generateShortCode();
                attempts = 0;
            }
        } while (repository.findByShortCode(shortCode).isPresent());

        return shortCode;
    }

    private String validateAndUseCustomCode(String customCode) {
        if (!Base62Encoder.isValidBase62(customCode)) {
            throw new InvalidUrlException("Custom short code must contain only alphanumeric characters");
        }

        if (customCode.length() < 3 || customCode.length() > 10) {
            throw new InvalidUrlException("Custom short code must be between 3 and 10 characters");
        }

        if (repository.findByShortCode(customCode).isPresent()) {
            throw new DuplicateShortCodeException("Short code already exists: " + customCode);
        }

        return customCode;
    }

    private ShortenUrlResponse buildResponse(ShortenedUrl url) {
        return ShortenUrlResponse.builder()
            .shortCode(url.getShortCode())
            .shortUrl(baseUrl + "/" + url.getShortCode())
            .originalUrl(url.getOriginalUrl())
            .createdAt(url.getCreatedAt())
            .expiresAt(url.getExpiresAt())
            .clickCount(url.getClickCount())
            .isActive(url.getIsActive())
            .build();
    }

    private void incrementClickCountAsync(ShortenedUrl url) {
        new Thread(() -> {
            try {
                url.incrementClickCount();
                repository.save(url);
                cacheService.updateInCache(url);
                log.debug("Click count incremented for short code: {}", url.getShortCode());
            } catch (Exception e) {
                log.error("Error incrementing click count", e);
            }
        }).start();
    }

    private synchronized long getNextCounter() {
        return ++idCounter;
    }

    private void initializeCounter() {
        try {
            idCounter = System.currentTimeMillis();
        } catch (Exception e) {
            log.error("Error initializing counter", e);
            idCounter = System.currentTimeMillis();
        }
    }
}