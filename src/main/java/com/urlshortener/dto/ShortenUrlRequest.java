package com.urlshortener.dto;

import lombok.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortenUrlRequest {
    @NotBlank(message = "URL is required")
    @URL(message = "Invalid URL format")
    private String originalUrl;
    private String customShortCode;
    private LocalDateTime expiresAt;
}
