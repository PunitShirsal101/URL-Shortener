package com.shortscale.repository;

import com.shortscale.model.UrlMapping;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import redis.embedded.RedisServer;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RedisUrlRepositoryTest {

    private static RedisServer redisServer;

    @BeforeAll
    static void startRedis() throws Exception {
        redisServer = new RedisServer();
        redisServer.start();
    }

    @AfterAll
    static void stopRedis() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> redisServer.ports().get(0));
    }

    @Autowired
    private RedisUrlRepository repository;

    @Test
    public void testSaveAndFind() {
        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode("test123");
        mapping.setOriginalUrl("https://example.com");
        mapping.setCreatedAt(LocalDateTime.now());
        mapping.setClickCount(0);

        repository.save(mapping);

        UrlMapping found = repository.findByShortCode("test123");
        assertNotNull(found);
        assertEquals("https://example.com", found.getOriginalUrl());
        assertEquals("test123", found.getShortCode());
    }

    @Test
    public void testExists() {
        assertFalse(repository.existsByShortCode("nonexistent"));

        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode("exists");
        mapping.setOriginalUrl("https://exists.com");
        mapping.setCreatedAt(LocalDateTime.now());
        mapping.setClickCount(0);

        repository.save(mapping);

        assertTrue(repository.existsByShortCode("exists"));
    }
}
