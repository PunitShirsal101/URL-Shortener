package com.shortscale.api.dto;

import lombok.Data;

@Data
public class ShortenRequest {
    private String originalUrl;
    private String customShortCode;
    private Integer ttlSeconds; // time to live in seconds
}
