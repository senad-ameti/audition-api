package com.audition.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Production cache configuration using Caffeine. Controlled by 'audition.cache.enabled'.
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "audition.cache.enabled", havingValue = "true", matchIfMissing = true)
public class CacheConfiguration {

    private static final int CACHE_TTL_MINUTES = 15;
    private static final int CACHE_MAX_SIZE = 1000;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("posts", "postsById", "comments",
            "postWithComments");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(CACHE_TTL_MINUTES))
            .maximumSize(CACHE_MAX_SIZE));
        return cacheManager;
    }
}
