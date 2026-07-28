package com.project.url_shortener.controller;

import com.project.url_shortener.dto.ShortenResponse;
import com.project.url_shortener.service.UrlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class RedirectController {
    UrlService urlService;
    public RedirectController(UrlService urlService) {
        this.urlService = urlService;
    }
    @GetMapping("/{shortCode}")
    public ResponseEntity<ShortenResponse> redirect(@PathVariable String shortCode) {
        String longUrl = urlService.getLongUrl(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(longUrl))
                .build();
    }
}
