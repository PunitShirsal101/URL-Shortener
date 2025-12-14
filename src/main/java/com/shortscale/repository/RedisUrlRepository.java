package com.shortscale.repository;

import com.shortscale.model.UrlMapping;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisUrlRepository {
    private final RedisTemplate<String, UrlMapping> redisTemplate;

    public RedisUrlRepository(RedisTemplate<String, UrlMapping> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public UrlMapping save(UrlMapping urlMapping) {
        redisTemplate.opsForValue().set(urlMapping.getShortCode(), urlMapping);
        return urlMapping;
    }

    public UrlMapping findByShortCode(String shortCode) {
        return redisTemplate.opsForValue().get(shortCode);
    }

    public boolean existsByShortCode(String shortCode) {
        return redisTemplate.hasKey(shortCode);
    }
}
