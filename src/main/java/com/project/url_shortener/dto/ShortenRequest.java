package com.project.url_shortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter@Setter
public class ShortenRequest {
    @NotBlank(message="longUrl must not be blank")
    @Pattern(regexp = "^https?://.+", message="longUrl must start with http:// or https://")
    private String longUrl;
}
