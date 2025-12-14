package com.shortscale.repository;

import com.shortscale.model.UrlMapping;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
public class RedisUrlRepositoryTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private RedisUrlRepository repository;

    @Test
    public void testSaveAndFind() {
        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode("test123");
        mapping.setOriginalUrl("http://example.com");
        mapping.setCreatedAt(LocalDateTime.now());
        mapping.setClickCount(0);

        repository.save(mapping);

        UrlMapping found = repository.findByShortCode("test123");
        assertNotNull(found);
        assertEquals("http://example.com", found.getOriginalUrl());
        assertEquals("test123", found.getShortCode());
    }

    @Test
    public void testExists() {
        assertFalse(repository.existsByShortCode("nonexistent"));

        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode("exists");
        mapping.setOriginalUrl("http://exists.com");
        mapping.setCreatedAt(LocalDateTime.now());
        mapping.setClickCount(0);

        repository.save(mapping);

        assertTrue(repository.existsByShortCode("exists"));
    }
}
