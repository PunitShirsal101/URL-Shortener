package com.shortscale.service;

import com.shortscale.api.dto.BulkShortenRequest;
import com.shortscale.api.dto.BulkShortenResponse;
import com.shortscale.api.dto.ShortenRequest;
import com.shortscale.api.dto.ShortenResponse;
import com.shortscale.model.UrlMapping;
import com.shortscale.repository.RedisUrlRepository;
import com.shortscale.util.HashGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnableAutoConfiguration(exclude = {KafkaAutoConfiguration.class, RedisAutoConfiguration.class})
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

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @Test
    public void shouldShortenUrlWithCustomCode() {
        ShortenRequest request = new ShortenRequest();
        request.setOriginalUrl("https://example.com");
        request.setCustomShortCode("abc123");

        Mockito.when(repository.existsByShortCode("abc123")).thenReturn(false);
        Mockito.when(hashGenerator.generateShortCode()).thenReturn("generated");

        ShortenResponse response = urlService.shortenUrl(request);

        assertNotNull(response);
        assertEquals("http://localhost:8080/abc123", response.getShortUrl());
        assertEquals("https://example.com", response.getOriginalUrl());
        assertEquals("abc123", response.getShortCode());

        Mockito.verify(repository).save(Mockito.any());
    }

    @Test
    public void shouldReturnNullWhenShortCodeNotFound() {
        Mockito.when(repository.findByShortCode("abc123")).thenReturn(null);
        String originalUrl = urlService.getOriginalUrl("abc123");
        assertNull(originalUrl);
    }

    @Test
    public void shouldThrowExceptionWhenCustomShortCodeExists() {
        ShortenRequest request = new ShortenRequest();
        request.setOriginalUrl("https://example.com");
        request.setCustomShortCode("abc123");

        Mockito.when(repository.existsByShortCode("abc123")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> urlService.shortenUrl(request));
    }

    @Test
    public void shouldSetExpirationWhenTtlProvided() {
        ShortenRequest request = new ShortenRequest();
        request.setOriginalUrl("https://example.com");
        request.setCustomShortCode("abc123");
        request.setTtlSeconds(3600);

        Mockito.when(repository.existsByShortCode("abc123")).thenReturn(false);

        ShortenResponse response = urlService.shortenUrl(request);

        assertNotNull(response);
        assertEquals("http://localhost:8080/abc123", response.getShortUrl());

        Mockito.verify(repository).save(Mockito.argThat(urlMapping -> urlMapping.getExpiresAt() != null));
    }

    @Test
    public void shouldNotSetExpirationWhenTtlIsZero() {
        ShortenRequest request = new ShortenRequest();
        request.setOriginalUrl("https://example.com");
        request.setCustomShortCode("abc123");
        request.setTtlSeconds(0);

        Mockito.when(repository.existsByShortCode("abc123")).thenReturn(false);

        ShortenResponse response = urlService.shortenUrl(request);

        assertNotNull(response);
        assertEquals("http://localhost:8080/abc123", response.getShortUrl());

        Mockito.verify(repository).save(Mockito.argThat(urlMapping -> urlMapping.getExpiresAt() == null));
    }

    @Test
    public void shouldGenerateShortCodeWhenNotProvided() {
        ShortenRequest request = new ShortenRequest();
        request.setOriginalUrl("https://example.com");
        // no customShortCode

        Mockito.when(repository.existsByShortCode("generated")).thenReturn(false);
        Mockito.when(hashGenerator.generateShortCode()).thenReturn("generated");

        ShortenResponse response = urlService.shortenUrl(request);

        assertNotNull(response);
        assertEquals("http://localhost:8080/generated", response.getShortUrl());
        assertEquals("https://example.com", response.getOriginalUrl());
        assertEquals("generated", response.getShortCode());

        Mockito.verify(hashGenerator).generateShortCode();
        Mockito.verify(repository).save(Mockito.any());
    }

    @Test
    public void shouldReturnNullWhenUrlExpired() {
        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setOriginalUrl("https://example.com");
        urlMapping.setExpiresAt(LocalDateTime.now().minusSeconds(1)); // expired

        Mockito.when(repository.findByShortCode("abc123")).thenReturn(urlMapping);

        String originalUrl = urlService.getOriginalUrl("abc123");
        assertNull(originalUrl);
    }

    @Test
    public void shouldReturnOriginalUrlAndIncrementClickCount() {
        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setOriginalUrl("https://example.com");
        urlMapping.setClickCount(5);

        Mockito.when(repository.findByShortCode("abc123")).thenReturn(urlMapping);

        String originalUrl = urlService.getOriginalUrl("abc123");
        assertEquals("https://example.com", originalUrl);

        Mockito.verify(repository).save(urlMapping);
        Mockito.verify(messagingTemplate).convertAndSend(Mockito.eq("/topic/analytics"), Mockito.any(Object.class));
        Mockito.verify(kafkaTemplate).send(Mockito.eq("url-analytics"), Mockito.any(com.shortscale.api.dto.AnalyticsEvent.class));
    }

    @Test
    public void shouldReturnClickCount() {
        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setClickCount(10);

        Mockito.when(repository.findByShortCode("abc123")).thenReturn(urlMapping);

        int clickCount = urlService.getClickCount("abc123");
        assertEquals(10, clickCount);
    }

    @Test
    public void shouldReturnZeroWhenShortCodeNotFound() {
        Mockito.when(repository.findByShortCode("nonexistent")).thenReturn(null);

        int clickCount = urlService.getClickCount("nonexistent");
        assertEquals(0, clickCount);
    }

    @Test
    public void shouldHandleBulkShortenRequests() {
        BulkShortenRequest bulkRequest = new BulkShortenRequest();
        ShortenRequest req1 = new ShortenRequest();
        req1.setOriginalUrl("https://example1.com");
        req1.setCustomShortCode("code1");
        ShortenRequest req2 = new ShortenRequest();
        req2.setOriginalUrl("https://example2.com");
        req2.setCustomShortCode("code2");
        bulkRequest.setRequests(java.util.List.of(req1, req2));

        Mockito.when(repository.existsByShortCode("code1")).thenReturn(false);
        Mockito.when(repository.existsByShortCode("code2")).thenReturn(false);

        BulkShortenResponse response = urlService.bulkShortenUrls(bulkRequest);

        assertNotNull(response);
        assertEquals(2, response.getResponses().size());
        assertEquals("http://localhost:8080/code1", response.getResponses().get(0).getShortUrl());
        assertEquals("http://localhost:8080/code2", response.getResponses().get(1).getShortUrl());
    }
}
