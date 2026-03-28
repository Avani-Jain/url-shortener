package com.urlshortener.controller;

import com.urlshortener.service.UrlShorteningService;
import com.urlshortener.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/v1")
public class UrlController {

    private final UrlShorteningService shorteningService;

    @Autowired
    public UrlController(UrlShorteningService shorteningService) {
        this.shorteningService = shorteningService;
    }

    @PostMapping("/shorten")
    public ResponseEntity<ShortenUrlResponse> shortenUrl(
        @Valid @RequestBody ShortenUrlRequest request,
        HttpServletRequest httpRequest
    ) {
        String clientIp = getClientIp(httpRequest);
        log.info("Shorten request from IP: {}, URL: {}", clientIp, request.getOriginalUrl());

        ShortenUrlResponse response = shorteningService.shortenUrl(request, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{shortCode}")
    public RedirectView redirectUrl(
        @PathVariable String shortCode,
        HttpServletRequest request
    ) {
        log.info("Redirect request for short code: {} from IP: {}", shortCode, getClientIp(request));

        String originalUrl = shorteningService.resolveUrl(shortCode);
        RedirectView redirectView = new RedirectView();
        redirectView.setUrl(originalUrl);
        redirectView.setStatusCode(org.springframework.http.HttpStatus.MOVED_PERMANENTLY);

        return redirectView;
    }

    @GetMapping("/details/{shortCode}")
    public ResponseEntity<UrlDetailsResponse> getUrlDetails(
        @PathVariable String shortCode
    ) {
        log.info("Details request for short code: {}", shortCode);
        UrlDetailsResponse response = shorteningService.getUrlDetails(shortCode);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analytics/{shortCode}")
    public ResponseEntity<AnalyticsResponse> getAnalytics(
        @PathVariable String shortCode
    ) {
        log.info("Analytics request for short code: {}", shortCode);
        AnalyticsResponse response = shorteningService.getAnalytics(shortCode);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{shortCode}")
    public ResponseEntity<Void> deleteUrl(
        @PathVariable String shortCode
    ) {
        log.info("Delete request for short code: {}", shortCode);
        shorteningService.deleteUrl(shortCode);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("{ \"status\": \"UP\" }");
    }

    private String getClientIp(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getHeader("X-Real-IP");
        }
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }
        return clientIp;
    }
}