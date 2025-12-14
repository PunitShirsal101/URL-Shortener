package com.shortscale.service;

import com.shortscale.api.dto.BulkShortenRequest;
import com.shortscale.api.dto.BulkShortenResponse;
import com.shortscale.api.dto.ShortenRequest;
import com.shortscale.api.dto.ShortenResponse;

public interface UrlService {
    ShortenResponse shortenUrl(ShortenRequest request);
    String getOriginalUrl(String shortCode);
    int getClickCount(String shortCode);
    BulkShortenResponse bulkShortenUrls(BulkShortenRequest request);
}
