package com.audition.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for caching with Caffeine implementation. Sets up TTL-based caching with 15-minute expiration for
 * external API responses.
 */
@Configuration
@EnableCaching
public class CacheConfiguration {

    private static final int CACHE_TTL_MINUTES = 15;
    private static final int CACHE_MAX_SIZE = 1000;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "posts", "postsById", "comments", "postWithComments"
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(CACHE_TTL_MINUTES))
            .maximumSize(CACHE_MAX_SIZE));
        return cacheManager;
    }
}
