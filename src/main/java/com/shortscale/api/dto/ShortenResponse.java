package com.shortscale.api.dto;

import lombok.Data;

@Data
public class ShortenResponse {
    private String shortUrl;
    private String originalUrl;
    private String shortCode;
}
