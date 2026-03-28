package com.urlshortener.repository;

import com.urlshortener.domain.ShortenedUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface ShortenedUrlRepository extends JpaRepository<ShortenedUrl, Long> {
    Optional<ShortenedUrl> findByShortCode(String shortCode);
    Optional<ShortenedUrl> findByOriginalUrl(String originalUrl);

    @Query("SELECT u FROM ShortenedUrl u WHERE u.createdAt >= :startDate")
    List<ShortenedUrl> findUrlsCreatedAfter(LocalDateTime startDate);

    @Query("SELECT u FROM ShortenedUrl u WHERE u.expiresAt IS NOT NULL AND u.expiresAt < CURRENT_TIMESTAMP")
    List<ShortenedUrl> findExpiredUrls();

    @Query("SELECT COUNT(u) FROM ShortenedUrl u WHERE u.createdAt >= :startDate")
    Long countUrlsCreatedAfter(LocalDateTime startDate);
}