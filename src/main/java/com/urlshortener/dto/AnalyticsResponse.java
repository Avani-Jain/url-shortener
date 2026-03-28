package com.urlshortener.dto;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsResponse {
    private String shortCode;
    private String originalUrl;
    private Long totalClicks;
    private Long clicksToday;
    private Long clicksThisWeek;
    private Long clicksThisMonth;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    private Double avgClicksPerDay;
}
