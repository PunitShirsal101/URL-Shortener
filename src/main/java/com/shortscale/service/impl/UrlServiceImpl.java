package com.shortscale.service.impl;

import com.shortscale.api.dto.BulkShortenRequest;
import com.shortscale.api.dto.BulkShortenResponse;
import com.shortscale.api.dto.ShortenRequest;
import com.shortscale.api.dto.ShortenResponse;
import com.shortscale.model.UrlMapping;
import com.shortscale.api.dto.AnalyticsEvent;
import com.shortscale.repository.RedisUrlRepository;
import com.shortscale.service.UrlService;
import com.shortscale.util.HashGenerator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class UrlServiceImpl implements UrlService {

    @Autowired
    private RedisUrlRepository repository;

    @Autowired
    private HashGenerator hashGenerator;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private KafkaTemplate<String, AnalyticsEvent> kafkaTemplate;

    private final Counter urlShortenedCounter;
    private final Counter urlClickedCounter;

    @Autowired
    public UrlServiceImpl(MeterRegistry meterRegistry) {
        this.urlShortenedCounter = meterRegistry.counter("url_shortened_total");
        this.urlClickedCounter = meterRegistry.counter("url_clicked_total");
    }

    @Override
    public ShortenResponse shortenUrl(ShortenRequest request) {
        String shortCode = request.getCustomShortCode();
        if (shortCode == null || shortCode.isEmpty()) {
            shortCode = hashGenerator.generateShortCode();
        } else {
            if (repository.existsByShortCode(shortCode)) {
                throw new IllegalArgumentException("Custom short code already exists");
            }
        }

        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setId(System.currentTimeMillis()); // simple ID
        urlMapping.setShortCode(shortCode);
        urlMapping.setOriginalUrl(request.getOriginalUrl());
        urlMapping.setCreatedAt(LocalDateTime.now());
        if (request.getTtlSeconds() != null && request.getTtlSeconds() > 0) {
            urlMapping.setExpiresAt(LocalDateTime.now().plusSeconds(request.getTtlSeconds()));
        }
        urlMapping.setClickCount(0);

        repository.save(urlMapping);

        ShortenResponse response = new ShortenResponse();
        response.setShortUrl("http://localhost:8080/" + shortCode); // assuming port 8080
        response.setOriginalUrl(request.getOriginalUrl());
        response.setShortCode(shortCode);

        // Send Kafka event
        kafkaTemplate.send("url-analytics", new AnalyticsEvent(shortCode, LocalDateTime.now(), "shorten", null, null));
        urlShortenedCounter.increment(); // Increment the shorten URL counter

        return response;
    }

    @Override
    public String getOriginalUrl(String shortCode) {
        UrlMapping urlMapping = repository.findByShortCode(shortCode);
        if (urlMapping == null) {
            return null;
        }
        if (urlMapping.getExpiresAt() != null && LocalDateTime.now().isAfter(urlMapping.getExpiresAt())) {
            return null; // expired
        }
        urlMapping.setClickCount(urlMapping.getClickCount() + 1);
        repository.save(urlMapping); // update click count
        // Send real-time update
        messagingTemplate.convertAndSend("/topic/analytics", Map.of("shortCode", shortCode, "clickCount", urlMapping.getClickCount()));
        // Send Kafka event
        kafkaTemplate.send("url-analytics", new AnalyticsEvent(shortCode, LocalDateTime.now(), "click", null, null));
        urlClickedCounter.increment(); // Increment the clicked URL counter
        return urlMapping.getOriginalUrl();
    }

    @Override
    public int getClickCount(String shortCode) {
        UrlMapping urlMapping = repository.findByShortCode(shortCode);
        return urlMapping != null ? urlMapping.getClickCount() : 0;
    }

    @Override
    public BulkShortenResponse bulkShortenUrls(BulkShortenRequest request) {
        BulkShortenResponse response = new BulkShortenResponse();
        response.setResponses(request.getRequests().stream().map(this::shortenUrl).toList());
        return response;
    }
}
