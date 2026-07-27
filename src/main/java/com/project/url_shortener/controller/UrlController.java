package com.project.url_shortener.controller;

import com.project.url_shortener.dto.ShortenRequest;
import com.project.url_shortener.dto.ShortenResponse;
import com.project.url_shortener.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UrlController {
    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }
    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        String shortUrl = urlService.createShortUrl(request.getLongUrl());
        return ResponseEntity.ok(new ShortenResponse(shortUrl));
    }
}
