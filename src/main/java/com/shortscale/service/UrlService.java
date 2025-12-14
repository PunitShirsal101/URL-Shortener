package com.shortscale.service;

import com.shortscale.api.dto.*;
import com.shortscale.model.UrlMapping;
import com.shortscale.repository.RedisUrlRepository;
import com.shortscale.util.HashGenerator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class UrlService {

    private final RedisUrlRepository repository;
    private final HashGenerator hashGenerator;
    private final SimpMessagingTemplate messagingTemplate;
    private final KafkaTemplate<String, AnalyticsEvent> kafkaTemplate;
    private final Counter urlShortenedCounter;
    private final Counter urlClickedCounter;

    public UrlService(RedisUrlRepository repository, HashGenerator hashGenerator, SimpMessagingTemplate messagingTemplate, KafkaTemplate<String, AnalyticsEvent> kafkaTemplate, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.hashGenerator = hashGenerator;
        this.messagingTemplate = messagingTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.urlShortenedCounter = meterRegistry.counter("url_shortened_total");
        this.urlClickedCounter = meterRegistry.counter("url_clicked_total");
    }

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

    public int getClickCount(String shortCode) {
        UrlMapping urlMapping = repository.findByShortCode(shortCode);
        return urlMapping != null ? urlMapping.getClickCount() : 0;
    }

    public BulkShortenResponse bulkShortenUrls(BulkShortenRequest request) {
        BulkShortenResponse response = new BulkShortenResponse();
        response.setResponses(request.getRequests().stream().map(this::shortenUrl).toList());
        return response;
    }
}
