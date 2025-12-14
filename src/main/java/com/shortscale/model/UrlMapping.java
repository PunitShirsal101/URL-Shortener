package com.shortscale.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UrlMapping {
    private Long id;
    private String shortCode;
    private String originalUrl;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private int clickCount;
}
