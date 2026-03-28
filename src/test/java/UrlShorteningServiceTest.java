import com.urlshortener.domain.ShortenedUrl;
import com.urlshortener.dto.ShortenUrlRequest;
import com.urlshortener.dto.ShortenUrlResponse;
import com.urlshortener.repository.ShortenedUrlRepository;
import com.urlshortener.service.CacheService;
import com.urlshortener.service.TokenBucketRateLimiter;
import com.urlshortener.service.UrlShorteningService;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.exception.InvalidUrlException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("URL Shortening Service Tests")
class UrlShorteningServiceTest {

    @Mock
    private ShortenedUrlRepository repository;

    @Mock
    private CacheService cacheService;

    @Mock
    private TokenBucketRateLimiter rateLimiter;

    @InjectMocks
    private UrlShorteningService service;

    private String testUrl = "https://www.example.com/very/long/path";
    private String testShortCode = "abc123";
    private String testClientIp = "192.168.1.1";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(rateLimiter.isAllowed(anyString())).thenReturn(true);
    }

    @Test
    @DisplayName("Should successfully shorten a URL")
    void testShortenUrlSuccess() {
        ShortenUrlRequest request = ShortenUrlRequest.builder()
            .originalUrl(testUrl)
            .build();

        when(repository.findByOriginalUrl(testUrl)).thenReturn(Optional.empty());
        when(repository.findByShortCode(anyString())).thenReturn(Optional.empty());

        ShortenedUrl savedUrl = ShortenedUrl.builder()
            .id(1L)
            .shortCode(testShortCode)
            .originalUrl(testUrl)
            .clickCount(0L)
            .isActive(true)
            .build();

        when(repository.save(any(ShortenedUrl.class))).thenReturn(savedUrl);

        ShortenUrlResponse response = service.shortenUrl(request, testClientIp);

        assertNotNull(response);
        assertEquals(testUrl, response.getOriginalUrl());
        assertEquals(0L, response.getClickCount());
        assertTrue(response.getIsActive());

        verify(repository, times(1)).save(any(ShortenedUrl.class));
        verify(cacheService, times(1)).putInCache(any(ShortenedUrl.class));
    }

    @Test
    @DisplayName("Should reuse existing shortened URL")
    void testShortenUrlReuse() {
        ShortenUrlRequest request = ShortenUrlRequest.builder()
            .originalUrl(testUrl)
            .build();

        ShortenedUrl existingUrl = ShortenedUrl.builder()
            .id(1L)
            .shortCode(testShortCode)
            .originalUrl(testUrl)
            .clickCount(10L)
            .isActive(true)
            .build();

        when(repository.findByOriginalUrl(testUrl)).thenReturn(Optional.of(existingUrl));

        ShortenUrlResponse response = service.shortenUrl(request, testClientIp);

        assertNotNull(response);
        assertEquals(testUrl, response.getOriginalUrl());
        assertEquals(testShortCode, response.getShortCode());
        assertEquals(10L, response.getClickCount());

        verify(repository, never()).save(any(ShortenedUrl.class));
    }

    @Test
    @DisplayName("Should throw exception for empty URL")
    void testShortenUrlWithEmptyUrl() {
        ShortenUrlRequest request = ShortenUrlRequest.builder()
            .originalUrl("")
            .build();

        when(rateLimiter.isAllowed(anyString())).thenReturn(true);

        assertThrows(InvalidUrlException.class, () -> {
            service.shortenUrl(request, testClientIp);
        });
    }

    @Test
    @DisplayName("Should successfully resolve a URL")
    void testResolveUrlSuccess() {
        ShortenedUrl shortenedUrl = ShortenedUrl.builder()
            .id(1L)
            .shortCode(testShortCode)
            .originalUrl(testUrl)
            .clickCount(5L)
            .isActive(true)
            .build();

        when(cacheService.getFromCache(testShortCode)).thenReturn(Optional.empty());
        when(repository.findByShortCode(testShortCode)).thenReturn(Optional.of(shortenedUrl));

        String resolvedUrl = service.resolveUrl(testShortCode);

        assertEquals(testUrl, resolvedUrl);
        verify(repository, times(1)).findByShortCode(testShortCode);
        verify(cacheService, times(1)).putInCache(any(ShortenedUrl.class));
    }

    @Test
    @DisplayName("Should throw exception for non-existent short code")
    void testResolveUrlNotFound() {
        when(cacheService.getFromCache(testShortCode)).thenReturn(Optional.empty());
        when(repository.findByShortCode(testShortCode)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            service.resolveUrl(testShortCode);
        });
    }

    @Test
    @DisplayName("Should delete URL successfully")
    void testDeleteUrlSuccess() {
        ShortenedUrl shortenedUrl = ShortenedUrl.builder()
            .id(1L)
            .shortCode(testShortCode)
            .originalUrl(testUrl)
            .isActive(true)
            .build();

        when(repository.findByShortCode(testShortCode)).thenReturn(Optional.of(shortenedUrl));
        when(repository.save(any(ShortenedUrl.class))).thenReturn(shortenedUrl);

        service.deleteUrl(testShortCode);

        verify(repository, times(1)).save(any(ShortenedUrl.class));
        verify(cacheService, times(1)).invalidateCache(testShortCode);
        assertFalse(shortenedUrl.getIsActive());
    }
}