package com.shortscale.service;

import com.shortscale.api.dto.ShortenRequest;
import com.shortscale.api.dto.ShortenResponse;
import com.shortscale.repository.RedisUrlRepository;
import com.shortscale.util.HashGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {"spring.kafka.enabled=false", "spring.kafka.bootstrap-servers=", "spring.kafka.listener.concurrency=0"})
public class UrlServiceTest {

    @Autowired
    private UrlService urlService;

    @MockBean
    private RedisUrlRepository repository;

    @MockBean
    private HashGenerator hashGenerator;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @MockBean
    private KafkaTemplate<String, com.shortscale.api.dto.AnalyticsEvent> kafkaTemplate;

    @Test
    public void testShortenUrl() {
        ShortenRequest request = new ShortenRequest();
        request.setOriginalUrl("http://example.com");
        request.setCustomShortCode("abc123");

        Mockito.when(repository.existsByShortCode("abc123")).thenReturn(false);
        Mockito.when(hashGenerator.generateShortCode()).thenReturn("generated");

        ShortenResponse response = urlService.shortenUrl(request);

        assertNotNull(response);
        assertEquals("http://localhost:8080/abc123", response.getShortUrl());
        assertEquals("http://example.com", response.getOriginalUrl());
        assertEquals("abc123", response.getShortCode());

        Mockito.verify(repository).save(Mockito.any());
    }

    @Test
    public void testGetOriginalUrl() {
        Mockito.when(repository.findByShortCode("abc123")).thenReturn(null);

        String originalUrl = urlService.getOriginalUrl("abc123");

        assertNull(originalUrl);
    }
}
