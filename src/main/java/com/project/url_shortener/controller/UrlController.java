package com.project.url_shortener.controller;

import com.project.url_shortener.dto.ShortenRequest;
import com.project.url_shortener.dto.ShortenResponse;
import com.project.url_shortener.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//@RestController
//@RequestMapping("/api")
//public class UrlController {
//
//    private final UrlService urlService;
//
//    public UrlController(UrlService urlService) {
//        this.urlService = urlService;
//    }
//    @PostMapping("/shorten")
//    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
//        String shortUrl = urlService.createShortUrl(request.getLongUrl());
//        return ResponseEntity.ok(new ShortenResponse(shortUrl));
//    }
//}
@RestController
@RequestMapping("/api")
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }
    @GetMapping("/")
    public ResponseEntity<ShortenResponse> check(){
        return ResponseEntity.ok(new ShortenResponse("CODE WORKED"));
    }

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        String shortUrl = urlService.createShortUrl(request.getLongUrl());
        return ResponseEntity.ok(new ShortenResponse(shortUrl));
    }
}